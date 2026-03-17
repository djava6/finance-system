import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../core/utils/file_saver.dart';
import '../../core/models/transaction_model.dart';
import '../../core/providers/auth_provider.dart';
import '../../core/services/transaction_service.dart';
import 'add_transaction_screen.dart';
import 'edit_transaction_screen.dart';

class TransactionListScreen extends StatefulWidget {
  const TransactionListScreen({super.key});

  @override
  State<TransactionListScreen> createState() => _TransactionListScreenState();
}

class _TransactionListScreenState extends State<TransactionListScreen> {
  final _service = TransactionService();
  List<TransactionModel> _transactions = [];
  bool _loading = true;
  DateTime? _filterInicio;
  DateTime? _filterFim;

  final _currencyFormat =
      NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
  final _dateFormat = DateFormat('dd/MM/yyyy');

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final token = context.read<AuthProvider>().token!;
      _transactions = await _service.listar(token,
          inicio: _filterInicio, fim: _filterFim);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))),
        );
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _pickDateRange() async {
    final picked = await showDateRangePicker(
      context: context,
      firstDate: DateTime(2020),
      lastDate: DateTime.now().add(const Duration(days: 365)),
      initialDateRange: _filterInicio != null && _filterFim != null
          ? DateTimeRange(start: _filterInicio!, end: _filterFim!)
          : null,
    );
    if (picked != null) {
      setState(() {
        _filterInicio = picked.start;
        _filterFim = picked.end;
      });
      _load();
    }
  }

  void _clearFilter() {
    setState(() {
      _filterInicio = null;
      _filterFim = null;
    });
    _load();
  }

  Future<void> _exportarCsv() async {
    try {
      final token = context.read<AuthProvider>().token!;
      final bytes = await _service.exportarCsv(token);
      await saveFile(bytes, 'transacoes.csv');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('CSV exportado com sucesso')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))),
        );
      }
    }
  }

  Future<void> _deletar(TransactionModel t) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Excluir transação'),
        content: Text('Deseja excluir "${t.descricao}"?'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: const Text('Cancelar')),
          FilledButton(
              style: FilledButton.styleFrom(backgroundColor: Colors.red),
              onPressed: () => Navigator.pop(context, true),
              child: const Text('Excluir')),
        ],
      ),
    );
    if (confirmed == true) {
      final token = context.read<AuthProvider>().token!;
      await _service.deletar(token, t.id);
      _load();
    }
  }

  Future<void> _editar(TransactionModel t) async {
    final updated = await Navigator.push<bool>(
      context,
      MaterialPageRoute(builder: (_) => EditTransactionScreen(transaction: t)),
    );
    if (updated == true) _load();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Transações'),
        actions: [
          if (_filterInicio != null)
            IconButton(
              icon: const Icon(Icons.filter_alt_off),
              tooltip: 'Limpar filtro',
              onPressed: _clearFilter,
            ),
          IconButton(
            icon: const Icon(Icons.date_range),
            tooltip: 'Filtrar por período',
            onPressed: _pickDateRange,
          ),
          IconButton(
            icon: const Icon(Icons.download_outlined),
            tooltip: 'Exportar CSV',
            onPressed: _exportarCsv,
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () async {
          final added = await Navigator.push<bool>(
            context,
            MaterialPageRoute(builder: (_) => const AddTransactionScreen()),
          );
          if (added == true) _load();
        },
        icon: const Icon(Icons.add),
        label: const Text('Nova'),
      ),
      body: Column(
        children: [
          if (_filterInicio != null)
            Container(
              color: Theme.of(context).colorScheme.primaryContainer,
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
              child: Row(
                children: [
                  const Icon(Icons.filter_alt, size: 16),
                  const SizedBox(width: 8),
                  Text(
                    '${_dateFormat.format(_filterInicio!)} – ${_dateFormat.format(_filterFim!)}',
                    style: const TextStyle(fontSize: 13),
                  ),
                ],
              ),
            ),
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : _transactions.isEmpty
                    ? const Center(child: Text('Nenhuma transação encontrada.'))
                    : RefreshIndicator(
                        onRefresh: _load,
                        child: ListView.builder(
                          padding: const EdgeInsets.only(bottom: 80),
                          itemCount: _transactions.length,
                          itemBuilder: (_, i) {
                            final t = _transactions[i];
                            return Dismissible(
                              key: Key('t_${t.id}'),
                              direction: DismissDirection.endToStart,
                              background: Container(
                                color: Colors.red,
                                alignment: Alignment.centerRight,
                                padding: const EdgeInsets.only(right: 20),
                                child: const Icon(Icons.delete,
                                    color: Colors.white),
                              ),
                              confirmDismiss: (_) async {
                                await _deletar(t);
                                return false;
                              },
                              child: ListTile(
                                leading: CircleAvatar(
                                  backgroundColor: t.isReceita
                                      ? Colors.green.shade100
                                      : Colors.red.shade100,
                                  child: Icon(
                                    t.isReceita
                                        ? Icons.arrow_upward
                                        : Icons.arrow_downward,
                                    color: t.isReceita
                                        ? Colors.green
                                        : Colors.red,
                                  ),
                                ),
                                title: Text(t.descricao),
                                subtitle: Text(
                                    '${_dateFormat.format(t.data)}${t.categoria != null ? ' • ${t.categoria}' : ''}'),
                                trailing: Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    Text(
                                      _currencyFormat.format(t.valor),
                                      style: TextStyle(
                                        color: t.isReceita
                                            ? Colors.green
                                            : Colors.red,
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                    const SizedBox(width: 4),
                                    IconButton(
                                      icon: const Icon(Icons.edit_outlined,
                                          size: 18),
                                      onPressed: () => _editar(t),
                                    ),
                                  ],
                                ),
                              ),
                            );
                          },
                        ),
                      ),
          ),
        ],
      ),
    );
  }
}
