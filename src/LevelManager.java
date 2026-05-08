import java.awt.*;

/**
 * LevelManager — exatamente 3 fases.
 *
 * Regra de posicionamento ANTI-BUG:
 *   Se buildFloor(r, x1, x2, FLOOR_Y) → tile sólido em y=FLOOR_Y
 *   → inimigo posicionado em pixelY = (FLOOR_Y - 1) * 32   (um tile acima)
 *
 * Fase 1: Introdução — Walker + Shooter, mapa 60 cols
 * Fase 2: Intermediária — Walker + Ghost + Chaser, mapa 80 cols, mais buracos
 * Fase 3: Épica — todos + Bomber + Turret + Boss, mapa 120 cols, 3 seções
 */
class LevelManager {

    Room buildLevel(int level) {
        return switch (level) {
            case 1  -> buildLevel1();
            case 2  -> buildLevel2();
            default -> buildLevel3();
        };
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    /** Chão sólido: bloco de 4 tiles de altura */
    private void floor(Room r, int x1, int x2, int y) {
        for (int x = x1; x <= x2; x++)
            for (int d = 0; d < 4; d++)
                r.setTile(x, y + d, new Tile(TileType.SOLID));
    }

    /** Plataforma one-way (1 tile de altura) */
    private void plat(Room r, int x1, int x2, int y) {
        for (int x = x1; x <= x2; x++)
            r.setTile(x, y, new Tile(TileType.PLATFORM));
    }

    /** Parede sólida vertical */
    private void wall(Room r, int x, int y1, int y2) {
        for (int y = y1; y <= y2; y++)
            r.setTile(x, y, new Tile(TileType.SOLID));
    }

    /** Espinhos */
    private void spikes(Room r, int x1, int x2, int y) {
        for (int x = x1; x <= x2; x++)
            r.setTile(x, y, new Tile(TileType.SPIKE));
    }

    /** Fosso de lava/hazard */
    private void hazard(Room r, int x1, int x2, int y) {
        for (int x = x1; x <= x2; x++) {
            r.setTile(x, y, new Tile(TileType.HAZARD));
            r.setTile(x, y + 1, new Tile(TileType.SOLID));
            r.setTile(x, y + 2, new Tile(TileType.SOLID));
        }
    }

    /** Moedas em linha */
    private void coins(Room r, int x1, int x2, int tileY) {
        for (int x = x1; x <= x2; x += 2)
            r.addCollectible(x * 32 + 8, (tileY - 1) * 32 - 12, 1);
    }

    // ================================================================
    // FASE 1 — Introdução (60 cols × 20 rows)
    // Tipos: Walker (0), Shooter (1)
    // Dificuldade: baixa — buracos pequenos, inimigos poucos
    // ================================================================
    Room buildLevel1() {
        final int F = 15; // linha de chão principal
        Room r = new Room(60, 20);
        r.setTheme(Color.decode("#10091e"), Color.decode("#0d1830"), Color.decode("#4030a0"));

        // Seção 1: início seguro
        floor(r, 0, 12, F);

        // Buraco pequeno (cols 13-14) → apenas espinhos
        spikes(r, 13, 14, F);
        floor(r, 13, 14, F + 1); // base sólida sob espinhos

        // Seção 2
        floor(r, 15, 28, F);

        // Buraco real (cols 29-31)
        // sem chão → queda fatal

        // Seção 3
        floor(r, 32, 44, F);

        // Seção 4 (fim)
        floor(r, 46, 59, F);

        // Parede inicial (barreira esquerda)
        wall(r, 0, 0, F);

        // Plataformas
        plat(r, 5,  9,  F - 3);
        plat(r, 16, 20, F - 4);
        plat(r, 22, 26, F - 6);
        plat(r, 33, 37, F - 3);
        plat(r, 42, 46, F - 5);

        // Coletáveis
        r.addCollectible(22 * 32, (F - 7) * 32, 7);  // Fragmento de Estrela
        r.addCollectible(38 * 32, (F - 1) * 32 - 8, 3); // Poção
        coins(r, 5, 9,   F - 3);
        coins(r, 16, 20, F - 4);
        coins(r, 33, 37, F - 3);

        // Inimigos — TODOS sobre tile sólido verificado
        // Walker em seção 1 (floor y=15, pixelY = 14*32 = 448)
        r.addEnemy(new Enemy(8 * 32,  (F - 1) * 32, 0, 6));   // Walker
        // Walker em seção 2
        r.addEnemy(new Enemy(20 * 32, (F - 1) * 32, 0, 6));   // Walker
        // Shooter em seção 3 — posicionado longe da borda do buraco
        // Seção 3 começa em x=32, colocamos em x=36 (longe da borda de 29-31)
        r.addEnemy(new Enemy(36 * 32, (F - 1) * 32, 1, 8));   // Shooter
        // Walker na seção 4
        r.addEnemy(new Enemy(50 * 32, (F - 1) * 32, 0, 7));   // Walker

        // EXIT no final
        r.setTile(58, F - 1, new Tile(TileType.EXIT));
        return r;
    }

    // ================================================================
    // FASE 2 — Intermediária (80 cols × 22 rows)
    // Tipos: Walker (0), Shooter (1), Ghost (2), Chaser (3)
    // Dificuldade: média — múltiplos buracos, Ghost voador, Chaser
    // ================================================================
    Room buildLevel2() {
        final int F = 17; // linha de chão (mais fundo para altura extra)
        Room r = new Room(80, 22);
        r.setTheme(Color.decode("#0e1530"), Color.decode("#0a2040"), Color.decode("#0088dd"));

        // Seção 1 (plataforma inicial segura)
        floor(r, 0, 10, F);

        // Buraco (11-13) — queda fatal
        // Seção 2
        floor(r, 14, 24, F);

        // Buraco com lava (25-28)
        hazard(r, 25, 28, F);

        // Seção 3
        floor(r, 29, 42, F);

        // Buraco (43-46)
        // Seção 4
        floor(r, 47, 60, F);

        // Buraco (61-63)
        // Seção 5 (final)
        floor(r, 64, 79, F);

        // Paredes
        wall(r, 0, 0, F);

        // Plataformas (ajudam a cruzar buracos)
        plat(r, 11, 13, F - 4); // sobre buraco 1
        plat(r, 24, 28, F - 5); // sobre lava
        plat(r, 32, 36, F - 6);
        plat(r, 38, 43, F - 4);
        plat(r, 43, 46, F - 7); // sobre buraco 3
        plat(r, 50, 55, F - 5);
        plat(r, 60, 64, F - 4); // sobre buraco 4
        plat(r, 68, 73, F - 6);

        // Coletáveis
        r.addCollectible(33 * 32, (F - 7) * 32, 7);  // Fragmento de Estrela
        r.addCollectible(52 * 32, (F - 6) * 32, 8);  // Item de pulo (Chaser dropa também)
        r.addCollectible(10 * 32, (F - 1) * 32 - 8, 3); // Poção
        coins(r, 14, 20, F);
        coins(r, 32, 38, F - 6);
        coins(r, 50, 55, F - 5);

        // Espinhos adicionais
        spikes(r, 29, 30, F);  // início seção 3
        spikes(r, 64, 65, F);  // início seção 5

        // Inimigos — posicionamento anti-bug
        // Seção 1
        r.addEnemy(new Enemy(6 * 32,  (F-1)*32, 0, 7));   // Walker

        // Seção 2 — Shooter longe das bordas dos buracos (14 e 24)
        r.addEnemy(new Enemy(18 * 32, (F-1)*32, 1, 9));   // Shooter (x=18, longe de 14 e 24)

        // Ghost — voa livremente, não precisa de chão, mas posicionamos acima do chão
        r.addEnemy(new Enemy(35 * 32, (F-5)*32, 2, 10));  // Ghost (voa)

        // Chaser na seção 3 — tem comportamento de pulo, então OK
        Enemy chaser1 = new Enemy(38 * 32, (F-1)*32, 3, 10);
        chaser1.guaranteedDrop = 8; // dropa item de pulo
        r.addEnemy(chaser1);

        // Seção 4
        r.addEnemy(new Enemy(52 * 32, (F-1)*32, 1, 9));   // Shooter (longe das bordas 47 e 60)
        r.addEnemy(new Enemy(57 * 32, (F-1)*32, 0, 8));   // Walker

        // Ghost na seção 5
        r.addEnemy(new Enemy(70 * 32, (F-5)*32, 2, 11));  // Ghost

        // Chaser seção 5
        r.addEnemy(new Enemy(74 * 32, (F-1)*32, 3, 11));  // Chaser (longe da borda 64)

        // EXIT
        r.setTile(78, F-1, new Tile(TileType.EXIT));
        return r;
    }

    // ================================================================
    // FASE 3 — ÉPICA (120 cols × 24 rows) — 3 seções distintas
    // Tipos: todos (0-6)
    // Dificuldade: alta — fossos grandes, Boss no final, Bomber + Turret
    // ================================================================
    Room buildLevel3() {
        final int F = 19; // linha de chão principal
        Room r = new Room(120, 24);
        r.setTheme(Color.decode("#1a0a2a"), Color.decode("#200a20"), Color.decode("#cc00ff"));

        // ─── SEÇÃO 1: "Caverna de Cristal" (cols 0-38) ──────────────────
        floor(r, 0, 8, F);

        // Buraco (9-11)
        floor(r, 12, 20, F);
        spikes(r, 12, 13, F); // espinhos no início da seção 2

        // Buraco com lava (21-24)
        hazard(r, 21, 24, F);

        floor(r, 25, 38, F);

        // Plataformas seção 1
        plat(r, 8,  12, F-5);  // ponte sobre buraco 1
        plat(r, 14, 18, F-7);
        plat(r, 22, 25, F-6);  // ponte sobre lava
        plat(r, 28, 33, F-8);
        plat(r, 34, 38, F-5);

        // ─── SEÇÃO 2: "Abismo" (cols 39-78) ─────────────────────────────
        // Buraco gigante (39-55) — só plataformas flutuantes para cruzar
        // Sem chão entre 39 e 55!
        plat(r, 39, 42, F-4);
        plat(r, 44, 47, F-7);
        plat(r, 49, 52, F-10);
        plat(r, 53, 56, F-7);
        plat(r, 57, 60, F-4);

        floor(r, 56, 70, F);

        // Buraco (71-74)
        floor(r, 75, 78, F);

        // Plataformas seção 2
        plat(r, 62, 66, F-6);
        plat(r, 68, 72, F-8);
        plat(r, 73, 76, F-5);  // ponte sobre buraco

        // ─── SEÇÃO 3: "Trono do Boss" (cols 79-119) ──────────────────────
        floor(r, 79, 119, F);

        // Paredes laterais da arena do boss
        wall(r, 79, F-8, F);  // parede esquerda da arena
        wall(r, 119, 0, F);   // parede direita (borda)

        // Plataformas na arena do boss
        plat(r, 82, 86, F-5);
        plat(r, 90, 95, F-8);
        plat(r, 98, 103, F-5);
        plat(r, 107, 112, F-8);

        // Hazard no fundo da arena (faz o player não se esconder atrás do boss)
        hazard(r, 115, 118, F);

        // ─── Espinhos extras ─────────────────────────────────────────────
        spikes(r, 25, 26, F);
        spikes(r, 56, 57, F);
        spikes(r, 79, 80, F);  // entrada da arena

        // ─── Coletáveis ──────────────────────────────────────────────────
        r.addCollectible(28 * 32, (F-9)*32, 7);  // Fragmento de Estrela (alto, requer habilidade)
        r.addCollectible(50 * 32, (F-11)*32, 7); // 2º Fragmento (no meio do abismo)
        r.addCollectible(90 * 32, (F-9)*32, 7);  // 3º Fragmento (na arena do boss)
        r.addCollectible(6  * 32, (F-1)*32-8, 3); // Poção inicial
        r.addCollectible(62 * 32, (F-7)*32, 0);  // Cura no meio do abismo
        r.addCollectible(85 * 32, (F-6)*32, 3);  // Poção antes do boss
        coins(r, 14, 20, F);
        coins(r, 62, 68, F-6);
        coins(r, 82, 86, F-5);

        // ─── INIMIGOS — posicionamento anti-bug ──────────────────────────

        // SEÇÃO 1 — Introdução dos tipos
        r.addEnemy(new Enemy(5  * 32, (F-1)*32, 0,  8)); // Walker
        r.addEnemy(new Enemy(15 * 32, (F-1)*32, 1, 10)); // Shooter (longe das bordas 12,20)
        r.addEnemy(new Enemy(29 * 32, (F-1)*32, 0,  9)); // Walker
        r.addEnemy(new Enemy(34 * 32, (F-6)*32, 2, 12)); // Ghost (voa na plataforma)
        // Chaser removido a pedido do jogador (evita cair no abismo)

        // Bomber: posicionado sobre chão sólido, longe de buracos
        r.addEnemy(new Enemy(26 * 32, (F-1)*32, 5, 10)); // Bomber

        // SEÇÃO 2 — Abismo
        // Ghost voando sobre o abismo
        r.addEnemy(new Enemy(44 * 32, (F-8)*32, 2, 13)); // Ghost
        r.addEnemy(new Enemy(52 * 32, (F-11)*32, 2, 13)); // Ghost

        // Shooter na seção 2 sólida (cols 56-70) — longe das bordas 56 e 70
        r.addEnemy(new Enemy(60 * 32, (F-1)*32, 1, 12)); // Shooter
        // Chaser removido definitivamente para evitar o bug sob as estruturas

        // Turret: FIXO no chão — type 6, imobilizado pelo AI
        Enemy turret1 = new Enemy(76 * 32, (F-1)*32, 6, 15); // Turret
        r.addEnemy(turret1);

        // SEÇÃO 3 — Arena do Boss
        // Bombers patrulham as plataformas da arena
        r.addEnemy(new Enemy(82 * 32, (F-6)*32, 5, 12)); // Bomber (plataforma F-5)
        r.addEnemy(new Enemy(98 * 32, (F-6)*32, 5, 12)); // Bomber (plataforma F-5)

        // Ghosts na arena
        r.addEnemy(new Enemy(88 * 32, (F-9)*32, 2, 14)); // Ghost
        r.addEnemy(new Enemy(105* 32, (F-9)*32, 2, 14)); // Ghost

        // Turret na arena do boss
        Enemy turret2 = new Enemy(92 * 32, (F-1)*32, 6, 18); // Turret
        r.addEnemy(turret2);

        // Chaser agressivo na arena
        r.addEnemy(new Enemy(85 * 32, (F-1)*32, 3, 14)); // Chaser
        r.addEnemy(new Enemy(110* 32, (F-1)*32, 3, 14)); // Chaser

        // BOSS — no fundo da arena, 10 tiles de espaço à frente antes do hazard
        // Boss está em x=108, hazard começa em 115 → 7 tiles de gap → boss não cai
        Enemy boss = new Enemy(108 * 32, (F-1)*32, 4, 200); // HP aumentado significativamente (antes era 80)
        boss.points = 500;
        r.addEnemy(boss);

        // EXIT — após matar todos os inimigos, inclusive o Boss
        r.setTile(113, F-1, new Tile(TileType.EXIT)); // Movido do x=117 (onde havia lava) para x=113
        return r;
    }
}
