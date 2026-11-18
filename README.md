# EclipseSMP

Plugin oficial da Eclipse SMP para Paper (1.21.11 a 26.x).

## Recursos
- **/plugin, /plugins, /pl** mostram apenas "Eclipse SMP"
- **Imortalidade** (`/immortal`): o jogador toma dano, mas nunca fica abaixo de uma vida mínima configurável (padrão: meio coração)
- **Modo hardcore**: ao morrer o jogador dropa a cabeça e é banido na hora; colocar a cabeça no chão desbane
- **/revive** `<player>`: alternativa para administradores
- **Proteção inicial** de 15 min (primeiro login e ao reviver) + mensagem de boas-vindas
- **Identidade**: `/nickname` e `/skinset`
- **Som de morte** configurável (padrão: spawn do Wither no local da morte) com `/deathsound`

## Comandos e permissões
| Comando | Permissão |
|---|---|
| `/immortal <player\|set\|unset\|toggle\|list\|reload>` | `eclipse.immortal` |
| `/revive <player>` | `eclipse.revive` |
| `/nickname <player> <apelido\|reset>` | `eclipse.nickname` |
| `/skinset <player> <skin\|reset>` | `eclipse.skinset` |
| `/protection <give\|remove\|check> <player>` | `eclipse.protection` |
| `/deathsound <on\|off\|set\|volume\|pitch\|global\|test\|reset\|status>` | `eclipse.deathsound` |

Outras permissões: `eclipse.plugins.bypass` (ver a lista real de plugins) e `eclipse.deathban.exempt` (não ser banido ao morrer).

## Compilar
Requer Java 21+ e Maven:

```
mvn package
```

O jar sai em `target/EclipseSMP.jar`. Coloque na pasta `plugins/` do servidor.

## Configuração
Veja `src/main/resources/config.yml` (mensagens, vida mínima, ban, som de morte).
