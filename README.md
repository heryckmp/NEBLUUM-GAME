# NEBLUUM - Stellar Evolution Platformer

Um jogo de plataforma 2D com temática espacial e evolução estelar, feito em Java puro com Swing.

## Controles

| Tecla | Ação |
|-------|------|
| WASD / Setas | Mover |
| W / Espaço / Cima | Pular (duplo pulo) |
| J / Z | Atacar com espada |
| L / C | Disparar Raios (consome munição) |
| K / X | Dash (esquiva rápida, consome energia) |
| P / Esc | Pausar |

## Evolução Estelar

- **Fragmentos de Estrela**: Colete um em cada fase para transformar gradualmente o personagem.
- **Forma de Estrela**: Ao coletar os 7 fragmentos, você assume a forma estelar radiante.
- **Poder do Raio**: Itens de raio aumentam o dano em 5% e adicionam raios visuais à sua aura.

## Build e Execução

Requer **JDK 8+**:

```bash
javac src/*.java
java -cp src Main
```

## Features

- **7 níveis** com dificuldade progressiva e biomas diferenciados.
- **Obstáculos Dinâmicos**: Fossos de Lava, Ácido e cristais de espinho.
- **IA de Inimigos**: Walker, Shooter, Ghost, Chaser e o Black Hole BOSS.
- **Sistema de Combate**: Espada e Raios com prioridade de hit corrigida.
- **Efeitos Visuais**: Partículas, Aura pulsante, Heal Star Burst e Emerald Burst.
- **Física**: Gravidade, colisão precisa por tile e plataformas móveis.
