# EPIC 11/E06 runbook

O serviço é dono do ciclo do lote. O domínio de destino é acionado somente após `approved`; `dryRun=true` atualiza apenas checkpoint e preview. Um lote pode ser reiniciado com a mesma chave e checksum: o processamento retoma do último checkpoint e não cria uma segunda intenção.

Cada operação precisa de `X-Organization-Id`, bearer, `Idempotency-Key` para criação e `X-Correlation-Id` opcional. `UNIT_LINKED` exige `X-Unit-Id` no contexto e no manifesto. Qualquer mismatch de organização/unidade retorna `403` ou `404` sem revelar dados de outro escopo.

Para investigar falhas, consulte o lote, preserve o checksum e corrija a origem. Reexecute `/run`; não edite migrations ou checkpoint concluído. Aprovação exige revisão humana, motivo e checksum idêntico. Rollback é lógico (`rolled_back`) e mantém auditoria/checksum.

Uploads aceitam CSV UTF-8/ISO-8859-1 ou XLSX, até 10 MiB e 100.000 linhas. O scanner fake determinístico rejeita EICAR; produção deve substituir o adapter por um scanner real. Exportações mascaram email, telefone, CPF e CNPJ por padrão, geram URL assinada por 30 minutos e gravam finalidade, escopo, checksum, expiração e correlação na auditoria.

Erros operacionais seguem Problem Details: `400/401/403/404/409/413/422/429/503`. Nunca registrar conteúdo bruto, URL assinada, segredo ou PII em logs.
