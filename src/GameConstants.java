/**
 * Centraliza todas as constantes do jogo.
 * Elimina magic numbers espalhados pelo código.
 */
public final class GameConstants {

    private GameConstants() {}

    // ── Mundo ──────────────────────────────────────────────
    public static final int   TILE_SIZE = 32;
    public static final int   MAX_LEVEL = 3;

    // ── Física ─────────────────────────────────────────────
    public static final float GRAVITY      = 0.42f;
    public static final float TERMINAL_VEL = 14f;
    public static final float FRICTION     = 0.85f;

    // ── Movimento do Player ────────────────────────────────
    public static final float MOVE_SPEED        = 4.0f;
    public static final float JUMP_POWER        = -10.5f;
    public static final float ACCELERATION      = 0.5f;
    public static final int   COYOTE_FRAMES     = 6;
    public static final int   JUMP_BUFFER_FRAMES = 5;

    // ── Combate do Player ──────────────────────────────────
    public static final int   ATTACK_DURATION    = 8;
    public static final int   ATTACK_COOLDOWN    = 20;
    public static final int   DASH_COST          = 30;
    public static final int   DASH_COOLDOWN      = 45;
    public static final float DASH_SPEED         = 12f;
    public static final int   INVINCIBLE_ON_HIT  = 45;
    public static final int   INVINCIBLE_ON_DASH = 15;

    // ── Stats do Player ────────────────────────────────────
    public static final int MAX_HP             = 5;
    public static final int MAX_ENERGY         = 100;
    public static final int INITIAL_LIGHTNING  = 100;
    public static final int MAX_LIGHTNING_AMMO = 200;
    public static final int STAR_POWER_MAX     = 7;
    public static final int HITS_TO_LOSE_STAR  = 3;

    // ── Rodeo ──────────────────────────────────────────────
    public static final int RODEO_PASSIVE_INTERVAL = 60;
    public static final int RODEO_PASSIVE_DAMAGE   = 1;
    public static final int MAX_RODEO_FRAMES       = 300;

    // ── Morte por queda ────────────────────────────────────
    public static final int FALL_DEATH_Y = 600;
}
