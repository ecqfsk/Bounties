# Bounties

Plugin de **recompensas por eliminação** para Minecraft (Spigot/Paper).

Jogadores colocam um valor em outro jogador. Quem eliminar o alvo recebe a recompensa.

**Autor:** ecq  
**Versão:** 1.3  
**API:** 1.13+ (compatível com servidores 1.8–1.21, conforme materials/config)

---

## Funcionalidades

- Colocar, somar e remover recompensas
- **Contribuidores** com reembolso ao remover/substituir/expirar
- **Expiração** configurável + aviso prévio
- **Cooldown** e limites anti-spam
- **Efeitos visuais** no alvo (partículas, action bar, som, glow opcional)
- **Histórico** e **estatísticas** (`/bounty history`, `/bounty stats`)
- Coleta automática ao matar o alvo
- GUI interativa (`/bounty gui`)
- Ranking de recompensas e top assassinos
- Contador de kills
- Anúncios globais configuráveis
- Integração com economia via **Vault**
- Placeholders (**PlaceholderAPI**)
- Tag de top killer no chat (**nChat**, opcional)

---

## Dependências

| Plugin | Obrigatório | Função |
|--------|-------------|--------|
| [Vault](https://www.spigotmc.org/resources/vault.34315/) | Sim | Economia |
| Plugin de economia (EssentialsX, CMI, XConomy, etc.) | Sim | Saldo / saque / depósito |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Não | Placeholders |
| nChat | Não | Tag do top 1 assassino |

---

## Instalação

1. Instale o **Vault** e um plugin de economia.
2. Coloque `Bounties-1.3.jar` em `plugins/`.
3. Reinicie o servidor (ou use um plugin manager com cuidado).
4. Ajuste `plugins/Bounties/config.yml` se quiser.

### Build a partir do código

```bash
mvn clean package
```

O jar sai em `target/Bounties-1.3.jar`.

---

## Comandos

Aliases: `/bounty`, `/bounties`, `/recompensa`, `/recompensas`

| Comando | Descrição |
|---------|-----------|
| `/bounty set <jogador> <valor>` | Coloca recompensa no jogador |
| `/bounty add <jogador> <valor>` | Soma valor a uma recompensa existente |
| `/bounty remove <jogador>` | Remove a recompensa |
| `/bounty check [jogador]` | Consulta recompensa |
| `/bounty list` | Lista recompensas ativas |
| `/bounty top [quantidade]` | Ranking de recompensas |
| `/bounty assassinos [quantidade]` | Top assassinos |
| `/bounty kills [jogador]` | Ver kills |
| `/bounty history [jogador]` | Histórico de ações |
| `/bounty stats [jogador]` | Estatísticas (ganho/gasto/claims) |
| `/bounty gui` | Abre o menu gráfico |
| `/bounty help` | Ajuda |
| `/bounty reload` | Recarrega config (admin) |

---

## Permissões

| Permissão | Default | Descrição |
|-----------|---------|-----------|
| `bounties.use` | true | Usar `/bounty` |
| `bounties.set` | true | Colocar recompensa |
| `bounties.add` | true | Adicionar valor |
| `bounties.remove` | op | Remover recompensa |
| `bounties.check` | true | Verificar recompensa |
| `bounties.list` | true | Listar recompensas |
| `bounties.top` | true | Ranking de recompensas |
| `bounties.killtop` | true | Top assassinos |
| `bounties.kills` | true | Ver próprias kills |
| `bounties.kills.other` | true | Ver kills de outros |
| `bounties.gui` | true | Abrir GUI |
| `bounties.admin` | op | Acesso admin |
| `bounties.bypass` | op | Não pode receber recompensa |
| `bounties.*` | op | Todas as permissões |

---

## Configuração

Arquivo: `plugins/Bounties/config.yml`

Principais opções em `settings`:

- `min-amount` / `max-amount` — limites de valor  
- `placement-fee-percent` — taxa ao colocar recompensa  
- `expire-after-seconds` — expiração (`0` = nunca)  
- `cooldown-seconds` / `max-active-placed` / `max-amount-placed` — anti-spam  
- `refund-on-remove` / `refund-on-replace` / `refund-percent` — reembolsos  
- `visuals.*` — partículas, action bar, glow, sons  
- `history.*` — tamanho do histórico  

Mensagens, top killer e tag do nChat também são configuráveis no mesmo arquivo.

---

## Como funciona

1. Um jogador usa `/bounty set` (ou a GUI) e paga o valor (Vault).
2. A recompensa fica ativa no alvo.
3. Se outro jogador eliminar o alvo, o valor é transferido para o killer.
4. Kills e rankings são salvos e podem ser consultados por comando ou placeholders.

---

## Licença

Veja o arquivo [LICENSE](LICENSE) no repositório.
