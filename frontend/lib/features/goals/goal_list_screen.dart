import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../core/models/meta_model.dart';
import '../../core/services/meta_service.dart';

class GoalListScreen extends StatefulWidget {
  const GoalListScreen({super.key});

  @override
  State<GoalListScreen> createState() => _GoalListScreenState();
}

class _GoalListScreenState extends State<GoalListScreen> {
  final _service = MetaService();
  final _currency = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');

  List<MetaModel> _metas = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final list = await _service.listar();
      setState(() => _metas = list);
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erro ao carregar metas')),
        );
      }
    } finally {
      setState(() => _loading = false);
    }
  }

  void _showAddDialog() {
    final nomeCtrl = TextEditingController();
    final valorCtrl = TextEditingController();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Nova meta'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: nomeCtrl,
              decoration: const InputDecoration(labelText: 'Nome'),
            ),
            TextField(
              controller: valorCtrl,
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
              decoration:
                  const InputDecoration(labelText: 'Valor alvo (R\$)'),
            ),
          ],
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Cancelar')),
          TextButton(
            onPressed: () async {
              final nome = nomeCtrl.text.trim();
              final valor = double.tryParse(
                  valorCtrl.text.replaceAll(',', '.'));
              if (nome.isEmpty || valor == null || valor <= 0) return;
              Navigator.pop(ctx);
              try {
                await _service.criar(nome: nome, valorAlvo: valor);
                _load();
              } catch (e) {
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('$e')),
                  );
                }
              }
            },
            child: const Text('Salvar'),
          ),
        ],
      ),
    );
  }

  void _showDepositoDialog(MetaModel meta) {
    final valorCtrl = TextEditingController();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Depositar em "${meta.nome}"'),
        content: TextField(
          controller: valorCtrl,
          keyboardType:
              const TextInputType.numberWithOptions(decimal: true),
          decoration: const InputDecoration(labelText: 'Valor (R\$)'),
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Cancelar')),
          TextButton(
            onPressed: () async {
              final valor = double.tryParse(
                  valorCtrl.text.replaceAll(',', '.'));
              if (valor == null || valor <= 0) return;
              Navigator.pop(ctx);
              try {
                await _service.depositar(meta.id!, valor);
                _load();
              } catch (e) {
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('$e')),
                  );
                }
              }
            },
            child: const Text('Depositar'),
          ),
        ],
      ),
    );
  }

  Future<void> _deletar(MetaModel meta) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Remover meta'),
        content: Text('Remover a meta "${meta.nome}"?'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: const Text('Cancelar')),
          TextButton(
              onPressed: () => Navigator.pop(context, true),
              child: const Text('Remover')),
        ],
      ),
    );
    if (confirm != true) return;
    try {
      await _service.deletar(meta.id!);
      _load();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erro ao remover meta')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Metas Financeiras')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _metas.isEmpty
              ? const Center(child: Text('Nenhuma meta cadastrada'))
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView.builder(
                    itemCount: _metas.length,
                    itemBuilder: (_, i) {
                      final m = _metas[i];
                      final pct = m.percentual.clamp(0.0, 100.0);
                      return Card(
                        margin: const EdgeInsets.symmetric(
                            horizontal: 12, vertical: 6),
                        child: Padding(
                          padding: const EdgeInsets.all(12),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceBetween,
                                children: [
                                  Expanded(
                                    child: Text(
                                      m.nome ?? '',
                                      style: const TextStyle(
                                          fontWeight: FontWeight.bold,
                                          fontSize: 16),
                                    ),
                                  ),
                                  if (m.concluida)
                                    const Icon(Icons.check_circle,
                                        color: Colors.green),
                                  if (!m.concluida)
                                    IconButton(
                                      icon: const Icon(Icons.add_circle_outline),
                                      tooltip: 'Depositar',
                                      onPressed: () =>
                                          _showDepositoDialog(m),
                                    ),
                                  IconButton(
                                    icon: const Icon(Icons.delete_outline),
                                    onPressed: () => _deletar(m),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 8),
                              LinearProgressIndicator(
                                value: pct / 100,
                                color: m.concluida
                                    ? Colors.green
                                    : Theme.of(context).colorScheme.primary,
                                backgroundColor: Colors.grey.shade200,
                                minHeight: 8,
                              ),
                              const SizedBox(height: 4),
                              Text(
                                '${_currency.format(m.valorAtual)} / ${_currency.format(m.valorAlvo)} (${pct.toStringAsFixed(1)}%)',
                              ),
                              if (m.prazo != null)
                                Text(
                                  'Prazo: ${m.prazo}',
                                  style: const TextStyle(
                                      fontSize: 12,
                                      color: Colors.grey),
                                ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
                ),
      floatingActionButton: FloatingActionButton(
        onPressed: _showAddDialog,
        child: const Icon(Icons.add),
      ),
    );
  }
}
