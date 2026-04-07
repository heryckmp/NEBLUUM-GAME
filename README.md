# NEBLUUM - Cyber Platformer

Um jogo de plataforma 2D estilo cyberpunk feito em Java puro com Swing.

## Controles

| Tecla | Acao |
|-------|------|
| WASD / Setas | Mover |
| W / Space / Up | Pular (duplo pulo) |
| S / Down | Drop-through (atravessar plataforma) |
| Z / J | Atacar com espada |
| X / K | Dash (esquiva rapida, consome energia) |
| C / L | Usar pocao de vida |
| Q | Dropar bomba |
| P / Esc | Pausar |

## Build e Execucao

Requer **JDK 8+**:

```bash
cd src
javac *.java
java Main
```

## Features

- **5 niveis** com dificuldade progressiva
- **5 tipos de inimigos**: Walker, Jumper, Shooter, Chaser e BOSS
- **Sistema de combate**: espada com damage e knockback
- **Dash** com frames de invencibilidade
- **Drops**: life, coin, shield, potion, bomb, sword upgrade, spike
- **Inventario**: coins, bombs, health potions
- **Particulas** e efeitos visuais
- **Parallax backgrounds** com cenario cyberpunk
- **Sintese de som** basico para efeitos
- **Fisica completa**: gravidade, colisao por tile, plataformas
