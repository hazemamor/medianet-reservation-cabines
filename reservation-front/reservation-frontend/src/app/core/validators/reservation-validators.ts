import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Le jour selectionne doit etre aujourd'hui ou dans le futur (jamais un jour precedent),
 * conformement au cahier des charges (6.5/6.6 : reservation sur un seul jour a venir).
 * Ne compare que la partie "date" (jour), independamment de l'heure.
 */
export function dateNonPasseeValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const valeur = control.value as Date | null;
    if (!valeur) {
      return null; // laisser Validators.required gerer l'absence de valeur
    }
    const aujourdhui = new Date();
    aujourdhui.setHours(0, 0, 0, 0);
    const jourSaisi = new Date(valeur);
    jourSaisi.setHours(0, 0, 0, 0);

    return jourSaisi.getTime() < aujourdhui.getTime() ? { dateDansLePasse: true } : null;
  };
}

/**
 * Validateur de groupe : l'heure de debut doit etre strictement avant l'heure de fin,
 * sur le meme jour (une reservation se deroule sur une seule journee, 6.5/6.6).
 */
export function heureDebutAvantFinValidator(controleHeureDebut: string, controleHeureFin: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const heureDebut = group.get(controleHeureDebut)?.value as string | null;
    const heureFin = group.get(controleHeureFin)?.value as string | null;

    if (!heureDebut || !heureFin) {
      return null;
    }

    return heureDebut >= heureFin ? { heureDebutApresFin: true } : null;
  };
}
