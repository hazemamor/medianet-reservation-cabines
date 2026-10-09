import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { debounceTime } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { CabineService } from '../../../core/services/cabine.service';
import { EquipementService } from '../../../core/services/equipement.service';
import { ReservationService } from '../../../core/services/reservation.service';
import { RecommendationService } from '../../../core/services/recommendation.service';
import { Cabine } from '../../../core/models/cabine.model';
import { Equipement } from '../../../core/models/equipement.model';
import { RecommendationResponse } from '../../../core/models/recommendation.model';
import { dateNonPasseeValidator, heureDebutAvantFinValidator } from '../../../core/validators/reservation-validators';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

@Component({
  selector: 'app-cabine-list',
  standalone: true,
  imports: [
    CommonModule, RouterLink, ReactiveFormsModule, MatInputModule, MatProgressSpinnerModule,
    MatDatepickerModule, PageHeaderComponent
  ],
  templateUrl: './cabine-list.component.html'
})
export class CabineListComponent implements OnInit {

  private fb = inject(FormBuilder);
  private cabineService = inject(CabineService);
  private equipementService = inject(EquipementService);
  private reservationService = inject(ReservationService);
  private recommendationService = inject(RecommendationService);
  private snackBar = inject(MatSnackBar);

  cabines = signal<Cabine[]>([]);
  equipementsDisponibles = signal<Equipement[]>([]);
  chargement = signal(true);
  reservationEnCours = signal<number | null>(null);
  recommandation = signal<RecommendationResponse | null>(null);
  rechercheRecommandation = signal(false);

  /** IDs des cabines deja reservees sur le creneau (date/heure) actuellement selectionne. */
  cabinesIndisponiblesSurCreneau = signal<Set<number>>(new Set());

  /** Statistiques affichees en haut de page (Disponibles / Occupees / Maintenance). */
  stats = computed(() => {
    const liste = this.cabines();
    return {
      disponibles: liste.filter(c => c.etat === 'DISPONIBLE').length,
      occupees: liste.filter(c => c.etat === 'RESERVEE').length,
      maintenance: liste.filter(c => c.etat === 'MAINTENANCE').length
    };
  });

  statutLabel(cabine: Cabine): string {
    switch (cabine.etat) {
      case 'DISPONIBLE': return 'Disponible';
      case 'RESERVEE': return 'Occupee';
      case 'MAINTENANCE': return 'Maintenance';
      default: return cabine.etat;
    }
  }

  statutClasse(cabine: Cabine): string {
    switch (cabine.etat) {
      case 'DISPONIBLE': return 'badge-statut--dispo';
      case 'RESERVEE': return 'badge-statut--occupee';
      case 'MAINTENANCE': return 'badge-statut--maintenance';
      default: return '';
    }
  }

  equipementsAffiches(cabine: Cabine): string[] {
    return cabine.equipements.slice(0, 3);
  }

  nombreEquipementsSupplementaires(cabine: Cabine): number {
    return Math.max(0, cabine.equipements.length - 3);
  }

  toggleEquipement(index: number): void {
    const arr = this.rechercheForm.get('equipements') as FormArray;
    arr.at(index).setValue(!arr.at(index).value);
  }

  equipementActif(index: number): boolean {
    const valeurs = this.rechercheForm.value.equipements as boolean[] ?? [];
    return !!valeurs[index];
  }

  /** Empeche de selectionner un jour anterieur a aujourd'hui dans le mat-datepicker. */
  readonly filtreDateMin = (date: Date | null): boolean => {
    if (!date) return true;
    const aujourdhui = new Date();
    aujourdhui.setHours(0, 0, 0, 0);
    return date.getTime() >= aujourdhui.getTime();
  };

  // Une reservation se deroule sur UN SEUL jour (6.5/6.6 du cahier des charges) :
  // un seul champ date + une heure de debut et une heure de fin sur cette meme journee.
  rechercheForm = this.fb.group({
    nombrePersonnes: [null as number | null],
    dateJour: [null as Date | null, [Validators.required, dateNonPasseeValidator()]],
    heureDebut: ['09:00', Validators.required],
    heureFin: ['10:00', Validators.required],
    equipements: this.fb.array<boolean>([])
  }, { validators: heureDebutAvantFinValidator('heureDebut', 'heureFin') });

  ngOnInit(): void {
    this.chargerToutesLesCabines();
    this.equipementService.lister().subscribe(eqs => {
      this.equipementsDisponibles.set(eqs);
      const arr = this.rechercheForm.get('equipements') as FormArray;
      eqs.forEach(() => arr.push(this.fb.control(false)));
    });

    // Des que la date/l'heure change, on rafraichit automatiquement quelles cabines
    // affichees sont deja reservees sur ce creneau (sans attendre un clic sur "Rechercher").
    this.rechercheForm.valueChanges.pipe(debounceTime(300)).subscribe(() => {
      this.rafraichirDisponibiliteCreneau();
    });
  }

  private chargerToutesLesCabines(): void {
    this.chargement.set(true);
    this.cabineService.lister().subscribe({
      next: (data) => {
        this.cabines.set(data);
        this.chargement.set(false);
        this.rafraichirDisponibiliteCreneau();
      },
      error: () => this.chargement.set(false)
    });
  }

  private equipementsSelectionnes(): string[] {
    const valeurs = this.rechercheForm.value.equipements as boolean[] ?? [];
    return this.equipementsDisponibles()
      .filter((_, i) => valeurs[i])
      .map(eq => eq.nom);
  }

  /** Combine le jour (Date, issu du datepicker) et une heure "HH:mm" en ISO LocalDateTime. */
  private combinerDateHeure(jour: Date, heure: string): string {
    const annee = jour.getFullYear();
    const mois = String(jour.getMonth() + 1).padStart(2, '0');
    const jourNum = String(jour.getDate()).padStart(2, '0');
    const [h, m] = heure.split(':');
    return `${annee}-${mois}-${jourNum}T${h.padStart(2, '0')}:${m.padStart(2, '0')}:00`;
  }

  /** Retourne les dates ISO uniquement si le formulaire est valide (jour futur, heure debut < fin). */
  private datesValides(): { debut: string; fin: string } | null {
    if (this.rechercheForm.invalid) {
      return null;
    }
    const v = this.rechercheForm.value;
    if (!v.dateJour || !v.heureDebut || !v.heureFin) {
      return null;
    }
    return {
      debut: this.combinerDateHeure(v.dateJour, v.heureDebut),
      fin: this.combinerDateHeure(v.dateJour, v.heureFin)
    };
  }

  /**
   * Interroge le backend (recherche sur les seules dates, sans autres criteres) pour
   * savoir quelles cabines actuellement affichees sont deja reservees sur le creneau
   * choisi, afin de masquer le bouton "Reserver" et afficher un message a la place.
   */
  private rafraichirDisponibiliteCreneau(): void {
    const dates = this.datesValides();
    if (!dates) {
      this.cabinesIndisponiblesSurCreneau.set(new Set());
      return;
    }
    this.cabineService.rechercher({ dateDebut: dates.debut, dateFin: dates.fin }).subscribe({
      next: (disponibles) => {
        const idsDisponibles = new Set(disponibles.map(c => c.id));
        const indisponibles = new Set(
          this.cabines()
            .filter(c => c.etat === 'DISPONIBLE' && !idsDisponibles.has(c.id))
            .map(c => c.id)
        );
        this.cabinesIndisponiblesSurCreneau.set(indisponibles);
      },
      error: () => this.cabinesIndisponiblesSurCreneau.set(new Set())
    });
  }

  estIndisponibleSurCreneau(cabine: Cabine): boolean {
    return this.peutReserver() && this.cabinesIndisponiblesSurCreneau().has(cabine.id);
  }

  get erreurDatePassee(): boolean {
    return this.rechercheForm.get('dateJour')?.hasError('dateDansLePasse') ?? false;
  }

  get erreurHeures(): boolean {
    return this.rechercheForm.hasError('heureDebutApresFin');
  }

  rechercher(): void {
    this.rechercheForm.markAllAsTouched();
    const dates = this.datesValides();
    if (!dates) {
      this.snackBar.open('Verifie les dates/heures saisies (jour passe ou heure de debut apres l\'heure de fin)', 'Fermer', { duration: 5000 });
      return;
    }
    this.chargement.set(true);
    this.recommandation.set(null);
    const v = this.rechercheForm.value;
    this.cabineService.rechercher({
      nombrePersonnes: v.nombrePersonnes ?? undefined,
      equipements: this.equipementsSelectionnes(),
      dateDebut: dates.debut,
      dateFin: dates.fin
    }).subscribe({
      next: (data) => {
        this.cabines.set(data);
        this.chargement.set(false);
        // Tous les resultats retournes par la recherche sont deja disponibles sur ce creneau.
        this.cabinesIndisponiblesSurCreneau.set(new Set());
      },
      error: (err) => {
        this.chargement.set(false);
        this.snackBar.open(err.error?.message ?? 'Erreur lors de la recherche', 'Fermer', { duration: 5000 });
      }
    });
  }

  reinitialiser(): void {
    this.rechercheForm.reset({ heureDebut: '09:00', heureFin: '10:00' });
    this.recommandation.set(null);
    this.chargerToutesLesCabines();
  }

  demanderRecommandation(): void {
    this.rechercheForm.markAllAsTouched();
    const dates = this.datesValides();
    const v = this.rechercheForm.value;
    if (!v.nombrePersonnes || !dates) {
      this.snackBar.open('Renseigne le nombre de participants et une date/heure valide pour obtenir une recommandation', 'Fermer', { duration: 5000 });
      return;
    }
    this.rechercheRecommandation.set(true);
    this.recommendationService.recommander(v.nombrePersonnes, this.equipementsSelectionnes(), dates.debut, dates.fin)
      .subscribe({
        next: (rec) => { this.recommandation.set(rec); this.rechercheRecommandation.set(false); },
        error: (err) => {
          this.rechercheRecommandation.set(false);
          this.snackBar.open(err.error?.message ?? 'Aucune recommandation disponible', 'Fermer', { duration: 5000 });
        }
      });
  }

  peutReserver(): boolean {
    return this.datesValides() !== null;
  }

  reserver(cabine: Cabine): void {
    this.rechercheForm.markAllAsTouched();
    const dates = this.datesValides();
    if (!dates) {
      this.snackBar.open('Verifie la date et les heures avant de reserver (jour passe ou heure de debut apres l\'heure de fin)', 'Fermer', { duration: 5000 });
      return;
    }
    this.reservationEnCours.set(cabine.id);
    this.reservationService.reserver({ cabineId: cabine.id, dateDebut: dates.debut, dateFin: dates.fin }).subscribe({
      next: () => {
        this.reservationEnCours.set(null);
        this.snackBar.open(`Cabine ${cabine.nom} reservee avec succes`, 'OK', { duration: 4000 });
        this.rafraichirDisponibiliteCreneau();
      },
      error: (err) => {
        this.reservationEnCours.set(null);
        // Conflit detecte au moment de la reservation (race condition avec un autre
        // employe) : on rafraichit l'etat pour masquer immediatement le bouton.
        if (err.status === 409) {
          this.rafraichirDisponibiliteCreneau();
        }
        this.snackBar.open(err.error?.message ?? 'Erreur lors de la reservation', 'Fermer', { duration: 5000 });
      }
    });
  }
}
