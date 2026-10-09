import { AfterViewInit, Component, ElementRef, ViewChild, ViewChildren, QueryList, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../../core/services/auth.service';
import { environment } from '../../../../environments/environment';

// Google Identity Services expose un objet global "google" charge via <script> dans index.html.
// Pas de @types dediee installee (evite une dependance supplementaire) : on type en "any".
declare const google: any;

type Etape = 'EMAIL' | 'OTP';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatSnackBarModule],
  templateUrl: './login.component.html'
})
export class LoginComponent implements AfterViewInit {

  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  @ViewChildren('otpInput') otpInputs!: QueryList<ElementRef<HTMLInputElement>>;
  @ViewChild('googleBtn') googleBtnRef?: ElementRef<HTMLDivElement>;

  etape = signal<Etape>('EMAIL');
  chargement = signal(false);

  /** Les 6 chiffres du code OTP, un par case (comme la maquette Figma). */
  otpDigits = signal<string[]>(['', '', '', '', '', '']);

  emailForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });

  get emailSaisi(): string {
    return this.emailForm.value.email ?? '';
  }

  get codeComplet(): string {
    return this.otpDigits().join('');
  }

  demanderOtp(): void {
    if (this.emailForm.invalid) {
      return;
    }
    this.chargement.set(true);
    this.authService.demanderOtp(this.emailSaisi).subscribe({
      next: (res) => {
        this.chargement.set(false);
        this.etape.set('OTP');
        this.otpDigits.set(['', '', '', '', '', '']);
        this.snackBar.open(res.message, 'OK', { duration: 4000 });
        setTimeout(() => this.otpInputs?.first?.nativeElement.focus(), 0);
      },
      error: (err) => {
        this.chargement.set(false);
        this.snackBar.open(err.error?.message ?? 'Erreur lors de la demande du code', 'Fermer', { duration: 5000 });
      }
    });
  }

  /**
   * Saisie normale (un chiffre par case). Gere aussi le cas ou plus d'un caractere
   * arrive dans l'evenement input (ex: autofill mobile qui ignore maxlength) en
   * distribuant les chiffres supplementaires vers les cases suivantes.
   */
  onDigitInput(index: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    let brut = input.value.replace(/\D/g, '');

    if (!brut) {
      const digits = [...this.otpDigits()];
      digits[index] = '';
      this.otpDigits.set(digits);
      return;
    }

    // Bug connu sur certains claviers mobiles : une seule frappe declenche parfois
    // deux evenements "input" consecutifs, dupliquant le meme chiffre (ex: taper
    // "2" affiche "22"). On neutralise ce cas precis : si tous les caracteres
    // recus sont identiques, on ne garde qu'une seule occurrence.
    if (brut.length > 1 && new Set(brut).size === 1) {
      brut = brut[0];
      input.value = brut;
    }

    this.distribuerChiffres(index, brut);
  }

  /**
   * Gere le collage (Ctrl+V / coller sur mobile) du code entier : distribue chaque
   * chiffre du presse-papiers dans les cases a partir de celle ciblee, plutot que
   * de ne garder que le dernier caractere colle dans une seule case.
   */
  onPaste(index: number, event: ClipboardEvent): void {
    event.preventDefault();
    const texte = event.clipboardData?.getData('text') ?? '';
    const chiffres = texte.replace(/\D/g, '');
    if (chiffres) {
      this.distribuerChiffres(index, chiffres);
    }
  }

  private distribuerChiffres(indexDepart: number, chiffres: string): void {
    const digits = [...this.otpDigits()];
    let i = indexDepart;
    for (const ch of chiffres) {
      if (i > 5) break;
      digits[i] = ch;
      i++;
    }
    this.otpDigits.set(digits);

    const dernierIndexRempli = Math.min(i, 6) - 1;

    // Le changement de focus est differe au tick suivant (setTimeout) pour ne pas
    // interferer avec le traitement en cours de l'evenement natif du clavier —
    // c'est cette interference qui provoquait la duplication du chiffre.
    setTimeout(() => {
      if (dernierIndexRempli < 5) {
        this.otpInputs.get(dernierIndexRempli + 1)?.nativeElement.focus();
      } else {
        this.otpInputs.get(5)?.nativeElement.blur();
      }
    }, 0);

    if (digits.every(d => d !== '')) {
      this.verifierOtp();
    }
  }

  onDigitKeydown(index: number, event: KeyboardEvent): void {
    if (event.key === 'Backspace' && !this.otpDigits()[index] && index > 0) {
      this.otpInputs.get(index - 1)?.nativeElement.focus();
    }
  }

  /**
   * Avant toute validation, on relit directement la valeur reelle affichee dans
   * chaque case (le DOM) plutot que de faire confiance uniquement au signal interne.
   * Ca protege contre tout mecanisme de remplissage automatique (autofill mobile,
   * gestionnaire de mots de passe...) qui aurait rempli les cases visuellement sans
   * declencher les evenements (input) attendus par Angular.
   */
  private synchroniserDepuisDom(): void {
    if (!this.otpInputs || this.otpInputs.length === 0) {
      return;
    }
    const valeursReelles = this.otpInputs.map(ref => (ref.nativeElement.value || '').replace(/\D/g, '').slice(0, 1));
    this.otpDigits.set(valeursReelles);
  }

  verifierOtp(): void {
    this.synchroniserDepuisDom();

    if (this.codeComplet.length !== 6) {
      this.snackBar.open('Saisis les 6 chiffres du code', 'Fermer', { duration: 4000 });
      return;
    }
    this.chargement.set(true);
    this.authService.verifierOtp(this.emailSaisi, this.codeComplet).subscribe({
      next: () => {
        this.chargement.set(false);
        this.router.navigate(['/cabines']);
      },
      error: (err) => {
        this.chargement.set(false);
        this.otpDigits.set(['', '', '', '', '', '']);
        setTimeout(() => this.otpInputs?.first?.nativeElement.focus(), 0);
        this.snackBar.open(err.error?.message ?? 'Code invalide', 'Fermer', { duration: 5000 });
      }
    });
  }

  renvoyerCode(): void {
    this.demanderOtp();
  }

  /**
   * CRUCIAL : sans ce trackBy, *ngFor suit les items par VALEUR (comportement par
   * defaut d'Angular pour les tableaux de primitives). Comme otpDigits demarre a
   * ['','','','','',''] (6 valeurs identiques), Angular ne peut pas distinguer les
   * cases entre elles et reutilise/deplace les noeuds DOM de facon incorrecte des
   * qu'une valeur change — c'est ce qui causait "taper 2 affiche 22" : l'evenement
   * (input) capture dans la fermeture un index qui ne correspond plus a la bonne
   * case apres reutilisation du noeud DOM par Angular. Suivre par index fixe
   * chaque <input> a sa position, quelle que soit sa valeur.
   */
  trackByIndex(index: number): number {
    return index;
  }

  retourEmail(): void {
    this.etape.set('EMAIL');
    this.otpDigits.set(['', '', '', '', '', '']);
  }

  ngAfterViewInit(): void {
    this.initialiserGoogleSignIn();
  }

  /**
   * Le script Google (charge en async/defer dans index.html) peut ne pas etre
   * pret immediatement : on reessaie brievement jusqu'a ce que "google" existe.
   */
  private initialiserGoogleSignIn(tentative = 0): void {
    if (typeof google === 'undefined' || !google?.accounts?.id) {
      if (tentative < 20) {
        setTimeout(() => this.initialiserGoogleSignIn(tentative + 1), 150);
      }
      return;
    }

    if (!environment.googleClientId || environment.googleClientId.includes('REMPLACE_PAR')) {
      // Pas configure : on n'affiche simplement pas le bouton, sans bloquer le reste du login.
      return;
    }

    google.accounts.id.initialize({
      client_id: environment.googleClientId,
      callback: (reponse: { credential: string }) => this.onGoogleCredential(reponse)
    });

    if (this.googleBtnRef && this.etape() === 'EMAIL') {
      google.accounts.id.renderButton(this.googleBtnRef.nativeElement, {
        theme: 'outline', size: 'large', width: 336, text: 'continue_with', locale: 'fr'
      });
    }
  }

  private onGoogleCredential(reponse: { credential: string }): void {
    if (!reponse?.credential) {
      this.snackBar.open('Connexion Google annulée', 'Fermer', { duration: 4000 });
      return;
    }
    this.chargement.set(true);
    this.authService.connexionAvecGoogle(reponse.credential).subscribe({
      next: () => {
        this.chargement.set(false);
        this.router.navigate(['/cabines']);
      },
      error: (err) => {
        this.chargement.set(false);
        this.snackBar.open(err.error?.message ?? 'Erreur lors de la connexion Google', 'Fermer', { duration: 5000 });
      }
    });
  }
}
