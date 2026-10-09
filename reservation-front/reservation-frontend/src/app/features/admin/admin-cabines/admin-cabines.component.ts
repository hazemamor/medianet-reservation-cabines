import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CabineService } from '../../../core/services/cabine.service';
import { EquipementService } from '../../../core/services/equipement.service';
import { Cabine } from '../../../core/models/cabine.model';
import { Equipement } from '../../../core/models/equipement.model';
import { PageHeaderComponent } from '../../../shared/page-header/page-header.component';

@Component({
  selector: 'app-admin-cabines',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatProgressSpinnerModule, PageHeaderComponent],
  templateUrl: './admin-cabines.component.html'
})
export class AdminCabinesComponent implements OnInit {

  private fb = inject(FormBuilder);
  private cabineService = inject(CabineService);
  private equipementService = inject(EquipementService);

  cabines = signal<Cabine[]>([]);
  equipementsDisponibles = signal<Equipement[]>([]);
  chargement = signal(true);
  enEdition = signal<Cabine | null>(null);
  toast = signal<string | null>(null);

  nbDisponibles = computed(() => this.cabines().filter(c => c.etat === 'DISPONIBLE').length);
  nbMaintenance = computed(() => this.cabines().filter(c => c.etat === 'MAINTENANCE').length);

  form = this.fb.group({
    nom: ['', Validators.required],
    numero: ['', Validators.required],
    capacite: [1, [Validators.required, Validators.min(1)]],
    description: [''],
    localisation: ['', Validators.required],
    photoUrl: [''],
    heureDisponibiliteDebut: ['09:00', Validators.required],
    heureDisponibiliteFin: ['18:00', Validators.required],
    equipements: this.fb.array<boolean>([])
  });

  ngOnInit(): void {
    this.charger();
    this.equipementService.lister().subscribe(eqs => {
      this.equipementsDisponibles.set(eqs);
      const arr = this.form.get('equipements') as FormArray;
      eqs.forEach(() => arr.push(this.fb.control(false)));
    });
  }

  private afficherToast(message: string): void {
    this.toast.set(message);
    setTimeout(() => this.toast.set(null), 2500);
  }

  private charger(): void {
    this.chargement.set(true);
    this.cabineService.lister().subscribe({
      next: (data) => { this.cabines.set(data); this.chargement.set(false); },
      error: () => this.chargement.set(false)
    });
  }

  editer(cabine: Cabine): void {
    this.enEdition.set(cabine);
    const valeurs = this.equipementsDisponibles().map(eq => cabine.equipements.includes(eq.nom));
    this.form.setValue({
      nom: cabine.nom,
      numero: cabine.numero,
      capacite: cabine.capacite,
      description: cabine.description ?? '',
      localisation: cabine.localisation,
      photoUrl: cabine.photoUrl ?? '',
      heureDisponibiliteDebut: cabine.heureDisponibiliteDebut?.substring(0, 5) ?? '09:00',
      heureDisponibiliteFin: cabine.heureDisponibiliteFin?.substring(0, 5) ?? '18:00',
      equipements: valeurs
    });
  }

  annulerEdition(): void {
    this.enEdition.set(null);
    this.form.reset({ capacite: 1, heureDisponibiliteDebut: '09:00', heureDisponibiliteFin: '18:00' });
  }

  toggleEquipement(index: number): void {
    const arr = this.form.get('equipements') as FormArray;
    arr.at(index).setValue(!arr.at(index).value);
  }

  equipementActif(index: number): boolean {
    const valeurs = this.form.value.equipements as boolean[] ?? [];
    return !!valeurs[index];
  }

  private equipementsSelectionnes(): string[] {
    const valeurs = this.form.value.equipements as boolean[] ?? [];
    return this.equipementsDisponibles().filter((_, i) => valeurs[i]).map(eq => eq.nom);
  }

  enregistrer(): void {
    if (this.form.invalid) {
      return;
    }
    const v = this.form.value;
    const payload = {
      nom: v.nom ?? '',
      numero: v.numero ?? '',
      capacite: v.capacite ?? 1,
      description: v.description ?? '',
      localisation: v.localisation ?? '',
      etat: 'DISPONIBLE' as const,
      equipements: this.equipementsSelectionnes(),
      photoUrl: v.photoUrl || null,
      heureDisponibiliteDebut: v.heureDisponibiliteDebut ?? '09:00',
      heureDisponibiliteFin: v.heureDisponibiliteFin ?? '18:00'
    };
    const enEdition = this.enEdition();
    const obs = enEdition ? this.cabineService.modifier(enEdition.id, payload) : this.cabineService.creer(payload);

    obs.subscribe({
      next: () => {
        this.afficherToast(enEdition ? 'Cabine mise à jour' : 'Cabine créée avec succès');
        this.annulerEdition();
        this.charger();
      },
      error: (err) => this.afficherToast(err.error?.message ?? 'Erreur')
    });
  }

  supprimer(cabine: Cabine): void {
    if (!confirm(`Supprimer la cabine "${cabine.nom}" ?`)) {
      return;
    }
    this.cabineService.supprimer(cabine.id).subscribe({
      next: () => { this.afficherToast('Cabine supprimée'); this.charger(); },
      error: (err) => this.afficherToast(err.error?.message ?? 'Erreur lors de la suppression')
    });
  }

  basculerMaintenance(cabine: Cabine): void {
    const obs = cabine.etat === 'MAINTENANCE'
      ? this.cabineService.remettreDisponible(cabine.id)
      : this.cabineService.mettreEnMaintenance(cabine.id);

    obs.subscribe({
      next: () => this.charger(),
      error: (err) => this.afficherToast(err.error?.message ?? 'Erreur')
    });
  }
}
