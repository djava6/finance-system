import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../core/models/orcamento_model.dart';
import '../../core/services/orcamento_service.dart';

class BudgetListScreen extends StatefulWidget {
  const BudgetListScreen({super.key});

  @override
  State<BudgetListScreen> createState() => _BudgetListScreenState();
}

class _BudgetListScreenState extends State<BudgetListScreen> {
  final _service = OrcamentoService();
  final _currency = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');

  List<OrcamentoModel> _orcamentos = [];
  bool _loading = true;
  late int _mes;
  late int _ano;

  @override
  void initState() {
    super.initState();
    final now = DateTime.now();
    _mes = now.month;
    _ano = now.year;
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final list = await _service.listarPorMes(_mes, _ano);
      setState(() => _orcamentos = list);
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erro ao carregar orçamentos')),
        );
      }
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _deletar(OrcamentoModel o) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Remover orçamento'),
        content: Text(
            'Remover orçamento de ${o.categoria ?? 'Sem categoria'}?'),
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
      await _service.deletar(o.id!);
      _load();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erro ao remover orçamento')),
        );
      }
    }
  }

  void _showAddDialog() {
    final limiteCtrl = TextEditingController();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Novo orçamento'),
        content: TextField(
          controller: limiteCtrl,
          keyboardType:
              const TextInputType.numberWithOptions(decimal: true),
          decoration:
              const InputDecoration(labelText: 'Valor limite (R\$)'),
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Cancelar')),
          TextButton(
            onPressed: () async {
              final valor = double.tryParse(
                  limiteCtrl.text.replaceAll(',', '.'));
              if (valor == null || valor <= 0) return;
              Navigator.pop(ctx);
              try {
                await _service.criar(
                    valorLimite: valor, mes: _mes, ano: _ano);
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

  Color _progressColor(double pct) {
    if (pct >= 100) return Colors.red;
    if (pct >= 80) return Colors.orange;
    return Colors.green;
  }

  @override
  Widget build(BuildContext context) {
    final monthLabel =
        DateFormat('MMMM yyyy', 'pt_BR').format(DateTime(_ano, _mes));
    return Scaffold(
      appBar: AppBar(
        title: Text('Orçamentos — $monthLabel'),
        actions: [
          IconButton(
            icon: const Icon(Icons.chevron_left),
            tooltip: 'Mês anterior',
            onPressed: () {
              setState(() {
                if (_mes == 1) {
                  _mes = 12;
                  _ano--;
                } else {
                  _mes--;
                }
              });
              _load();
            },
          ),
          IconButton(
            icon: const Icon(Icons.chevron_right),
            tooltip: 'Próximo mês',
            onPressed: () {
              setState(() {
                if (_mes == 12) {
                  _mes = 1;
                  _ano++;
                } else {
                  _mes++;
                }
              });
              _load();
            },
          ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _orcamentos.isEmpty
              ? const Center(child: Text('Nenhum orçamento para este mês'))
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView.builder(
                    itemCount: _orcamentos.length,
                    itemBuilder: (_, i) {
                      final o = _orcamentos[i];
                      final pct = o.percentual.clamp(0.0, 100.0);
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
                                  Text(
                                    o.categoria ?? 'Sem categoria',
                                    style: const TextStyle(
                                        fontWeight: FontWeight.bold),
                                  ),
                                  IconButton(
                                    icon: const Icon(Icons.delete_outline),
                                    onPressed: () => _deletar(o),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 8),
                              LinearProgressIndicator(
                                value: pct / 100,
                                color: _progressColor(o.percentual),
                                backgroundColor: Colors.grey.shade200,
                                minHeight: 8,
                              ),
                              const SizedBox(height: 4),
                              Text(
                                '${_currency.format(o.gasto)} / ${_currency.format(o.valorLimite)} (${pct.toStringAsFixed(1)}%)',
                                style: TextStyle(
                                    color: _progressColor(o.percentual)),
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
