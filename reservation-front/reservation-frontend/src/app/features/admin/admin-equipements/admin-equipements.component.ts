import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EquipementService } from '../../../core/services/equipement.service';
import { Equipement } from '../../../core/models/equipement.model';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

/** Emoji associe a chaque equipement connu, pour un rendu visuel plus parlant (comme la maquette). */
const ICONES_EQUIPEMENT: Record<string, string> = {
  'Projecteur 4K': '📽️', 'Télévision': '📺', 'Tableau blanc': '🗒️', 'Webcam': '📷',
  'Haut-parleurs': '🔊', 'Rallonge électrique': '🔌', 'Climatisation': '❄️',
  'Projecteur': '💡', 'Imprimante': '🖨️'
};

@Component({
  selector: 'app-admin-equipements',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatProgressSpinnerModule, PageHeaderComponent],
  templateUrl: './admin-equipements.component.html'
})
export class AdminEquipementsComponent implements OnInit {

  private fb = inject(FormBuilder);
  private equipementService = inject(EquipementService);

  equipements = signal<Equipement[]>([]);
  chargement = signal(true);
  enEdition = signal<Equipement | null>(null);
  toast = signal<string | null>(null);

  form = this.fb.group({
    nom: ['', Validators.required],
    description: ['']
  });

  ngOnInit(): void {
    this.charger();
  }

  private afficherToast(message: string): void {
    this.toast.set(message);
    setTimeout(() => this.toast.set(null), 2500);
  }

  iconePour(nom: string): string {
    return ICONES_EQUIPEMENT[nom] ?? '🔧';
  }

  private charger(): void {
    this.chargement.set(true);
    this.equipementService.lister().subscribe({
      next: (data) => { this.equipements.set(data); this.chargement.set(false); },
      error: () => this.chargement.set(false)
    });
  }

  editer(equipement: Equipement): void {
    this.enEdition.set(equipement);
    this.form.setValue({ nom: equipement.nom, description: equipement.description ?? '' });
  }

  annulerEdition(): void {
    this.enEdition.set(null);
    this.form.reset();
  }

  enregistrer(): void {
    if (this.form.invalid) {
      return;
    }
    const valeur = { nom: this.form.value.nom ?? '', description: this.form.value.description ?? '' };
    const enEdition = this.enEdition();

    const obs = enEdition ? this.equipementService.modifier(enEdition.id, valeur) : this.equipementService.creer(valeur);

    obs.subscribe({
      next: () => {
        this.afficherToast(enEdition ? 'Équipement mis à jour' : 'Équipement créé avec succès');
        this.annulerEdition();
        this.charger();
      },
      error: (err) => this.afficherToast(err.error?.message ?? 'Erreur')
    });
  }

  supprimer(equipement: Equipement): void {
    if (!confirm(`Supprimer l'equipement "${equipement.nom}" ?`)) {
      return;
    }
    this.equipementService.supprimer(equipement.id).subscribe({
      next: () => { this.afficherToast('Équipement supprimé'); this.charger(); },
      error: (err) => this.afficherToast(err.error?.message ?? 'Erreur lors de la suppression')
    });
  }
}
