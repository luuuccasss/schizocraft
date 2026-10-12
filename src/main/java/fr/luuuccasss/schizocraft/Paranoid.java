package fr.luuuccasss.schizocraft;

/** Interface ajoutée à ServerPlayer par mixin : la paranoïa du joueur, de 0 à 100. */
public interface Paranoid {
    float schizocraft$getParanoia();

    void schizocraft$setParanoia(float paranoia);

    default Stage schizocraft$getStage() {
        return Stage.of(schizocraft$getParanoia());
    }
}
