import java.awt.*;

class LevelManager {
    java.util.Random rand = new java.util.Random();

    Room buildLevel(int level) {
        if (level <= 1) return buildLevel1();
        else if (level == 2) return buildLevel2();
        else if (level == 3) return buildLevel3();
        else if (level == 4) return buildLevel4();
        else if (level == 5) return buildLevel5();
        else if (level == 6) return buildLevel6();
        else return buildFinalLevel();
    }

    private void buildFloor(Room r, int x1, int x2, int y) {
        for (int x = x1; x <= x2; x++) {
            r.setTile(x, y, new Tile(TileType.SOLID));
            for (int d = 1; d <= 3; d++) r.setTile(x, y + d, new Tile(TileType.SOLID));
        }
    }

    private void buildHazardPit(Room r, int x1, int x2, int y) {
        for (int x = x1; x <= x2; x++) {
            r.setTile(x, y, new Tile(TileType.HAZARD));
            for (int d = 1; d <= 2; d++) r.setTile(x, y + d, new Tile(TileType.SOLID));
        }
    }

    private void buildPlat(Room r, int x1, int x2, int y) {
        for (int x = x1; x <= x2; x++) r.setTile(x, y, new Tile(TileType.PLATFORM));
    }

    private void buildWall(Room r, int x, int y1, int y2) {
        for (int y = y1; y <= y2; y++) r.setTile(x, y, new Tile(TileType.SOLID));
    }

    private void addCoins(Room r, int x, int y, int count) {
        for (int i = 0; i < count; i++) r.addCollectible(x + i * 30, y, 1);
    }

    private void addPlatformCoins(Room r, int x1, int x2, int y) {
        for (int x = x1; x <= x2; x += 2) r.addCollectible(x * 32 + 16, y - 20, 1);
    }

    private void addSpikes(Room r, int x1, int x2, int y) {
        for (int x = x1; x <= x2; x++) r.setTile(x, y, new Tile(TileType.SPIKE));
    }

    Room buildLevel1() {
        int W = 60, H = 20;
        Room r = new Room(W, H);

        buildFloor(r, 0, 15, 16); 
        buildFloor(r, 18, 25, 16);
        buildFloor(r, 30, 45, 16);
        buildFloor(r, 50, 59, 16);

        buildPlat(r, 5, 9, 13);
        buildPlat(r, 14, 18, 11);
        buildPlat(r, 20, 25, 9);
        buildPlat(r, 30, 35, 12);
        buildPlat(r, 40, 44, 10);

        buildWall(r, 0, 0, 16);
        buildWall(r, 28, 14, 16);
        buildWall(r, 49, 14, 16);

        r.addEnemy(new Enemy(10 * 32, 15 * 32, 0, 7));  // Walker
        r.addEnemy(new Enemy(22 * 32, 15 * 32, 0, 7));  // Walker
        r.addEnemy(new Enemy(35 * 32, 15 * 32, 3, 9));  // Chaser

        r.addCollectible(20 * 32, 8 * 32, 7); // Fragmento 1
        r.addCollectible(38 * 32, 15 * 32 - 10, 3);
        addCoins(r, 3 * 32, 15 * 32 - 10, 5);

        addSpikes(r, 16, 17, 16); 
        addSpikes(r, 26, 29, 16);
        addSpikes(r, 46, 49, 16);

        r.setTile(58, 15, new Tile(TileType.EXIT));
        return r;
    }

    Room buildLevel2() {
        int W = 65, H = 20;
        Room r = new Room(W, H);
        r.setTheme(Color.decode("#1a1830"), Color.decode("#0d2040"), Color.decode("#00aaff"));

        buildFloor(r, 0, 8, 16);
        buildFloor(r, 12, 18, 14);
        buildFloor(r, 22, 30, 16);
        buildFloor(r, 35, 40, 12);
        buildFloor(r, 45, 52, 16);
        buildFloor(r, 57, 64, 16);

        buildPlat(r, 14, 18, 11);
        buildPlat(r, 20, 26, 9);
        buildPlat(r, 28, 34, 12);
        buildPlat(r, 32, 36, 8); // Ponto para o fragmento

        r.addCollectible(33 * 32, 7 * 32, 7); // Fragmento 2
        r.addEnemy(new Enemy(5 * 32,  15 * 32, 1,  8));  // Shooter
        r.addEnemy(new Enemy(18 * 32, 13 * 32, 0,  7));  // Walker no segundo bloco
        r.addEnemy(new Enemy(40 * 32, 11 * 32, 1,  8));  // Shooter
        r.addEnemy(new Enemy(58 * 32, 15 * 32, 3,  9));  // Chaser

        addSpikes(r, 9, 11, 16);
        addSpikes(r, 19, 21, 16);
        addSpikes(r, 31, 34, 16);

        r.setTile(63, 15, new Tile(TileType.EXIT));
        return r;
    }

    Room buildLevel3() {
        int W = 70, H = 22;
        Room r = new Room(W, H);
        r.setTheme(Color.decode("#2a1a30"), Color.decode("#2a1040"), Color.decode("#aa44ff"));

        buildFloor(r, 0, 6, 18);
        buildFloor(r, 10, 16, 16);
        buildFloor(r, 20, 26, 18);
        buildFloor(r, 30, 36, 14);
        buildFloor(r, 40, 48, 16);
        buildFloor(r, 52, 58, 18);

        buildPlat(r, 25, 30, 10);
        buildPlat(r, 32, 36, 9);

        r.addCollectible(34 * 32, 8 * 32, 7); // Fragmento 3
        r.addEnemy(new Enemy(12 * 32, 15 * 32, 2, 10));  // Ghost
        Enemy m2 = new Enemy(22 * 32, 17 * 32, 3,  9);
        m2.guaranteedDrop = 8; // Dropa o item de pulo!
        r.addEnemy(m2);  // Chaser
        r.addEnemy(new Enemy(40 * 32, 17 * 32, 0,  8));  // Walker
        r.addEnemy(new Enemy(46 * 32, 15 * 32, 3,  9));  // Chaser (fora do buraco)
        r.addEnemy(new Enemy(55 * 32, 17 * 32, 2, 10));  // Ghost (fora do buraco)

        addSpikes(r, 7, 9, 18);
        addSpikes(r, 27, 29, 18);

        r.setTile(68, 17, new Tile(TileType.EXIT));
        return r;
    }

    Room buildLevel4() {
        int W = 70, H = 20;
        Room r = new Room(W, H);
        r.setTheme(Color.decode("#3a1a1a"), Color.decode("#401a0d"), Color.decode("#ff6600"));

        buildFloor(r, 0, 5, 16);
        buildFloor(r, 8, 14, 16);
        buildFloor(r, 18, 24, 16);
        buildFloor(r, 28, 34, 16);
        buildFloor(r, 48, 55, 16);

        buildPlat(r, 15, 20, 9);
        buildPlat(r, 24, 30, 10);

        r.addCollectible(28 * 32, 9 * 32, 7); // Fragmento 4
        r.addEnemy(new Enemy(10 * 32, 15 * 32, 1, 10));  // Shooter
        r.addEnemy(new Enemy(20 * 32, 15 * 32, 3, 11));  // Chaser
        r.addEnemy(new Enemy(32 * 32, 15 * 32, 0,  9));  // Walker
        r.addEnemy(new Enemy(50 * 32, 15 * 32, 2, 10));  // Ghost
        r.addEnemy(new Enemy(60 * 32, 15 * 32, 1, 10));  // Shooter

        addSpikes(r, 6, 7, 16);
        addSpikes(r, 25, 27, 16);

        r.setTile(68, 15, new Tile(TileType.EXIT));
        return r;
    }

    Room buildLevel5() {
        int W = 75, H = 20;
        Room r = new Room(W, H);
        r.setTheme(Color.decode("#0a2a1a"), Color.decode("#0d4020"), Color.decode("#00ff88"));

        buildFloor(r, 0, 10, 16);
        buildHazardPit(r, 11, 15, 16);
        buildFloor(r, 16, 25, 16);
        buildHazardPit(r, 26, 30, 16);
        buildFloor(r, 31, 74, 16);

        buildPlat(r, 25, 31, 12);
        buildPlat(r, 40, 46, 12);

        r.addEnemy(new Enemy(20 * 32, 15 * 32, 2, 11));  // Ghost
        r.addEnemy(new Enemy(35 * 32, 15 * 32, 3, 12));  // Chaser
        r.addEnemy(new Enemy(55 * 32, 15 * 32, 1, 11));  // Shooter
        r.addEnemy(new Enemy(65 * 32, 15 * 32, 3, 12));  // Chaser
        r.addCollectible(43 * 32, 11 * 32, 7); // Fragmento 5
        r.addCollectible(20 * 32, 14 * 32, 3);

        r.setTile(73, 15, new Tile(TileType.EXIT));
        return r;
    }

    Room buildLevel6() {
        int W = 80, H = 20;
        Room r = new Room(W, H);
        r.setTheme(Color.decode("#1a0a2a"), Color.decode("#300d40"), Color.decode("#ff00ff"));

        buildFloor(r, 0, 5, 16);
        buildHazardPit(r, 6, 20, 16);
        buildFloor(r, 21, 30, 16);
        buildHazardPit(r, 31, 50, 16);
        buildFloor(r, 51, 79, 16);

        for (int i = 0; i < 5; i++) {
            buildPlat(r, 7 + i * 3, 9 + i * 3, 13 - (i % 2) * 3);
        }
        r.addCollectible(13 * 32, 10 * 32, 7); // Fragmento 6 (no ar)

        r.addEnemy(new Enemy(25 * 32, 15 * 32, 4, 22));  // Boss
        r.addEnemy(new Enemy(60 * 32, 15 * 32, 2, 13));  // Ghost
        r.addEnemy(new Enemy(70 * 32, 15 * 32, 3, 13));  // Chaser
        r.setTile(78, 15, new Tile(TileType.EXIT));
        return r;
    }

    Room buildFinalLevel() {
        int W = 80, H = 20;
        Room r = new Room(W, H);
        r.setTheme(Color.decode("#2a0a2a"), Color.decode("#200a18"), Color.decode("#ff00aa"));

        buildFloor(r, 0, 4, 16);
        buildHazardPit(r, 5, 70, 16); // Fosso gigante
        buildFloor(r, 71, 79, 16);

        buildPlat(r, 10, 15, 12);
        buildPlat(r, 20, 25, 9);
        buildPlat(r, 30, 35, 12);
        buildPlat(r, 40, 45, 9);
        buildPlat(r, 50, 55, 12);
        buildPlat(r, 60, 65, 9);

        r.addCollectible(42 * 32, 8 * 32, 7); // Fragmento 7 Final
        r.addEnemy(new Enemy(74 * 32, 14 * 32, 4, 55)); // Boss final
        r.addEnemy(new Enemy(30 * 32, 11 * 32, 2, 14)); // Ghost
        r.addEnemy(new Enemy(45 * 32, 11 * 32, 2, 14)); // Ghost
        r.addEnemy(new Enemy(60 * 32, 11 * 32, 1, 13)); // Shooter

        r.setTile(78, 15, new Tile(TileType.EXIT));
        return r;
    }
}
