import java.awt.*;

class LevelManager {
    java.util.Random rand = new java.util.Random();

    Room buildLevel(int level) {
        if (level <= 1) return buildLevel1();
        else if (level == 2) return buildLevel2();
        else if (level == 3) return buildLevel3();
        else if (level == 4) return buildLevel4();
        else return buildFinalLevel();
    }

    private void buildFloor(Room r, int x1, int x2, int y) {
        for (int x = x1; x <= x2; x++) {
            r.setTile(x, y, new Tile(TileType.SOLID));
            for (int d = 1; d <= 3; d++) r.setTile(x, y + d, new Tile(TileType.SOLID));
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

        buildFloor(r, 0, 12, 16);
        buildFloor(r, 18, 25, 16);
        buildFloor(r, 30, 45, 16);
        buildFloor(r, 50, 59, 16);

        buildPlat(r, 5, 9, 13);
        buildPlat(r, 14, 18, 11);
        buildPlat(r, 20, 25, 9);
        buildPlat(r, 30, 35, 12);
        buildPlat(r, 40, 44, 10);
        buildPlat(r, 28, 32, 14);

        buildWall(r, 0, 0, 16);
        buildWall(r, 28, 14, 16);
        buildWall(r, 49, 14, 16);

        r.addEnemy(new Enemy(10 * 32, 15 * 32, 0, 4));
        r.addEnemy(new Enemy(22 * 32, 15 * 32, 0, 4));
        r.addEnemy(new Enemy(35 * 32, 15 * 32, 3, 6));
        r.addEnemy(new Enemy(50 * 32, 15 * 32, 0, 4));

        addCoins(r, 3 * 32, 15 * 32 - 10, 5);
        addPlatformCoins(r, 14, 18, 11);
        r.addCollectible(20 * 32, 8 * 32, 0);
        r.addCollectible(38 * 32, 15 * 32 - 10, 3);
        r.addCollectible(45 * 32, 15 * 32 - 10, 2);

        addSpikes(r, 13, 17, 16);
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

        buildPlat(r, 3, 7, 12);
        buildPlat(r, 9, 13, 10);
        buildPlat(r, 14, 18, 11);
        buildPlat(r, 20, 26, 9);
        buildPlat(r, 28, 34, 12);
        buildPlat(r, 32, 36, 8);
        buildPlat(r, 38, 44, 10);
        buildPlat(r, 42, 48, 7);
        buildPlat(r, 48, 52, 12);
        buildPlat(r, 54, 60, 13);

        buildWall(r, 0, 14, 16);
        buildWall(r, 19, 12, 16);
        buildWall(r, 31, 8, 16);
        buildWall(r, 53, 13, 16);
        buildWall(r, 64, 14, 16);

        r.addEnemy(new Enemy(8 * 32, 15 * 32, 1, 5));
        r.addEnemy(new Enemy(24 * 32, 15 * 32, 2, 4));
        r.addEnemy(new Enemy(25 * 32, 8 * 32, 1, 5));
        r.addEnemy(new Enemy(40 * 32, 15 * 32, 3, 5));
        r.addEnemy(new Enemy(34 * 32, 11 * 32, 0, 3));
        r.addEnemy(new Enemy(55 * 32, 15 * 32, 2, 4));

        addCoins(r, 3 * 32, 14 * 32, 3);
        addCoins(r, 14 * 32, 13 * 32, 4);
        addCoins(r, 30 * 32, 11 * 32, 3);
        r.addCollectible(24 * 32 + 16, 8 * 32, 0);
        r.addCollectible(42 * 32 + 16, 6 * 32, 5);
        r.addCollectible(55 * 32, 15 * 32 - 10, 4);
        addPlatformCoins(r, 48, 52, 12);

        addSpikes(r, 9, 11, 16);
        addSpikes(r, 19, 21, 16);
        addSpikes(r, 31, 34, 16);
        addSpikes(r, 41, 44, 16);
        addSpikes(r, 53, 56, 16);

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
        buildFloor(r, 62, 69, 18);

        buildPlat(r, 3, 7, 14);
        buildPlat(r, 7, 11, 10);
        buildPlat(r, 11, 15, 12);
        buildPlat(r, 16, 20, 9);
        buildPlat(r, 21, 26, 14);
        buildPlat(r, 25, 30, 10);
        buildPlat(r, 32, 36, 9);
        buildPlat(r, 38, 44, 12);
        buildPlat(r, 42, 48, 8);
        buildPlat(r, 50, 56, 11);
        buildPlat(r, 54, 60, 13);
        buildPlat(r, 60, 66, 14);

        buildWall(r, 0, 16, 18);
        buildWall(r, 7, 14, 18);
        buildWall(r, 17, 12, 18);
        buildWall(r, 27, 14, 18);
        buildWall(r, 37, 10, 18);
        buildWall(r, 49, 14, 18);
        buildWall(r, 59, 16, 18);

        r.addEnemy(new Enemy(14 * 32, 15 * 32, 1, 6));
        r.addEnemy(new Enemy(22 * 32, 17 * 32, 3, 6));
        r.addEnemy(new Enemy(34 * 32, 13 * 32, 2, 5));
        r.addEnemy(new Enemy(35 * 32, 8 * 32, 2, 5));
        r.addEnemy(new Enemy(44 * 32, 15 * 32, 1, 5));
        r.addEnemy(new Enemy(50 * 32, 15 * 32, 3, 5));
        r.addEnemy(new Enemy(58 * 32, 17 * 32, 0, 4));
        r.addEnemy(new Enemy(60 * 32, 15 * 32, 1, 5));

        addCoins(r, 3 * 32, 13 * 32, 4);
        addCoins(r, 16 * 32, 8 * 32, 4);
        r.addCollectible(32 * 32 + 16, 8 * 32, 0);
        r.addCollectible(42 * 32, 7 * 32, 2);
        r.addCollectible(50 * 32, 10 * 32, 3);
        addPlatformCoins(r, 60, 66, 14);

        addSpikes(r, 7, 9, 18);
        addSpikes(r, 17, 19, 16);
        addSpikes(r, 27, 29, 18);
        addSpikes(r, 37, 39, 14);
        addSpikes(r, 49, 51, 16);
        addSpikes(r, 59, 61, 18);

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
        buildFloor(r, 38, 44, 16);
        buildFloor(r, 48, 55, 16);
        buildFloor(r, 59, 69, 16);

        buildPlat(r, 2, 6, 12);
        buildPlat(r, 6, 10, 9);
        buildPlat(r, 12, 16, 12);
        buildPlat(r, 15, 20, 9);
        buildPlat(r, 20, 25, 13);
        buildPlat(r, 24, 30, 10);
        buildPlat(r, 28, 34, 8);
        buildPlat(r, 34, 40, 11);
        buildPlat(r, 38, 44, 9);
        buildPlat(r, 45, 50, 12);
        buildPlat(r, 50, 56, 10);
        buildPlat(r, 57, 63, 11);
        buildPlat(r, 60, 66, 13);

        buildWall(r, 0, 14, 16);
        buildWall(r, 6, 14, 16);
        buildWall(r, 15, 14, 16);
        buildWall(r, 25, 14, 16);
        buildWall(r, 35, 14, 16);
        buildWall(r, 45, 14, 16);
        buildWall(r, 56, 14, 16);

        r.addEnemy(new Enemy(12 * 32, 15 * 32, 1, 7));
        r.addEnemy(new Enemy(20 * 32, 15 * 32, 3, 7));
        r.addEnemy(new Enemy(22 * 32, 8 * 32, 2, 6));
        r.addEnemy(new Enemy(30 * 32, 15 * 32, 3, 7));
        r.addEnemy(new Enemy(32 * 32, 7 * 32, 2, 6));
        r.addEnemy(new Enemy(40 * 32, 15 * 32, 1, 6));
        r.addEnemy(new Enemy(50 * 32, 15 * 32, 2, 6));
        r.addEnemy(new Enemy(52 * 32, 9 * 32, 1, 5));
        r.addEnemy(new Enemy(60 * 32, 15 * 32, 3, 6));
        r.addEnemy(new Enemy(66 * 32, 15 * 32, 1, 6));

        addCoins(r, 2 * 32, 11 * 32, 4);
        r.addCollectible(15 * 32 + 16, 8 * 32, 5);
        r.addCollectible(28 * 32 + 16, 9 * 32, 0);
        r.addCollectible(38 * 32, 10 * 32 - 10, 3);
        r.addCollectible(52 * 32, 8 * 32, 0);
        addPlatformCoins(r, 57, 63, 11);

        addSpikes(r, 6, 7, 16);
        addSpikes(r, 15, 17, 16);
        addSpikes(r, 25, 27, 16);
        addSpikes(r, 35, 37, 16);
        addSpikes(r, 45, 47, 16);
        addSpikes(r, 56, 58, 16);

        r.setTile(68, 15, new Tile(TileType.EXIT));
        return r;
    }

    Room buildFinalLevel() {
        int W = 80, H = 20;
        Room r = new Room(W, H);
        r.setTheme(Color.decode("#2a0a2a"), Color.decode("#200a18"), Color.decode("#ff00aa"));

        buildFloor(r, 0, 4, 16);
        buildFloor(r, 8, 12, 16);
        buildFloor(r, 16, 22, 14);
        buildFloor(r, 26, 32, 16);
        buildFloor(r, 36, 42, 16);
        buildFloor(r, 46, 52, 14);
        buildFloor(r, 56, 62, 16);
        buildFloor(r, 66, 79, 16);

        buildPlat(r, 2, 5, 13);
        buildPlat(r, 4, 8, 9);
        buildPlat(r, 8, 12, 11);
        buildPlat(r, 10, 14, 13);
        buildPlat(r, 12, 16, 10);
        buildPlat(r, 14, 20, 7);
        buildPlat(r, 20, 25, 9);
        buildPlat(r, 26, 32, 12);
        buildPlat(r, 30, 36, 9);
        buildPlat(r, 34, 40, 10);
        buildPlat(r, 38, 44, 7);
        buildPlat(r, 40, 46, 12);
        buildPlat(r, 46, 52, 9);
        buildPlat(r, 50, 56, 11);
        buildPlat(r, 54, 60, 12);
        buildPlat(r, 60, 66, 10);
        buildPlat(r, 64, 70, 12);
        buildPlat(r, 68, 75, 9);

        buildWall(r, 0, 14, 16);
        buildWall(r, 5, 12, 16);
        buildWall(r, 13, 10, 16);
        buildWall(r, 23, 12, 16);
        buildWall(r, 33, 14, 16);
        buildWall(r, 43, 12, 16);
        buildWall(r, 53, 10, 16);
        buildWall(r, 63, 14, 16);

        for (int i = 0; i < 6; i++) {
            r.addEnemy(new Enemy((8 + i * 10) * 32, 15 * 32, i % 3, 5 + i));
        }
        for (int i = 0; i < 4; i++) {
            r.addEnemy(new Enemy((12 + i * 14) * 32, 8 * 32, 2, 4 + i));
        }

        // Boss
        r.addEnemy(new Enemy(74 * 32, 14 * 32, 4, 40));
        r.addEnemy(new Enemy(72 * 32, 15 * 32, 3, 8));

        addCoins(r, 2 * 32, 12 * 32, 3);
        addCoins(r, 10 * 32, 12 * 32, 4);
        r.addCollectible(14 * 32, 6 * 32, 5);
        r.addCollectible(20 * 32, 8 * 32, 0);
        r.addCollectible(30 * 32, 8 * 32, 0);
        r.addCollectible(38 * 32, 6 * 32, 3);
        r.addCollectible(50 * 32, 10 * 32, 2);
        r.addCollectible(60 * 32, 9 * 32, 0);
        addPlatformCoins(r, 64, 70, 12);
        addPlatformCoins(r, 68, 75, 9);

        addSpikes(r, 5, 7, 16);
        addSpikes(r, 13, 15, 16);
        addSpikes(r, 23, 25, 16);
        addSpikes(r, 33, 35, 16);
        addSpikes(r, 43, 45, 16);
        addSpikes(r, 53, 55, 16);
        addSpikes(r, 63, 65, 16);

        r.setTile(78, 15, new Tile(TileType.EXIT));
        return r;
    }
}
