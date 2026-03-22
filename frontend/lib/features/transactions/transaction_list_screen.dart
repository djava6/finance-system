import 'package:file_picker/file_picker.dart';
import 'package:firebase_analytics/firebase_analytics.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../core/utils/file_saver.dart';
import '../../core/models/transaction_model.dart';
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
  final _scrollController = ScrollController();

  List<TransactionModel> _transactions = [];
  bool _loading = true;
  bool _loadingMore = false;
  bool _hasMore = true;
  int _currentPage = 0;

  DateTime? _filterInicio;
  DateTime? _filterFim;

  final _currencyFormat =
      NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
  final _dateFormat = DateFormat('dd/MM/yyyy');

  static const _pageSize = 20;

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    _load();
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
            _scrollController.position.maxScrollExtent - 200 &&
        !_loadingMore &&
        _hasMore) {
      _loadMore();
    }
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _transactions = [];
      _currentPage = 0;
      _hasMore = true;
    });
    try {
      final result = await _service.listar(
        inicio: _filterInicio,
        fim: _filterFim,
        page: 0,
        size: _pageSize,
      );
      if (mounted) {
        setState(() {
          _transactions = result.content;
          _hasMore = result.page + 1 < result.totalPages;
          _currentPage = result.page;
        });
      }
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

  Future<void> _loadMore() async {
    if (!_hasMore || _loadingMore) return;
    setState(() => _loadingMore = true);
    try {
      final result = await _service.listar(
        inicio: _filterInicio,
        fim: _filterFim,
        page: _currentPage + 1,
        size: _pageSize,
      );
      if (mounted) {
        setState(() {
          _transactions.addAll(result.content);
          _currentPage = result.page;
          _hasMore = result.page + 1 < result.totalPages;
        });
      }
    } catch (_) {
    } finally {
      if (mounted) setState(() => _loadingMore = false);
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
      await FirebaseAnalytics.instance.logEvent(name: 'filtro_periodo');
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
      final bytes = await _service.exportarCsv();
      await saveFile(bytes, 'transacoes.csv');
      await FirebaseAnalytics.instance.logEvent(name: 'exportar_csv');
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

  Future<void> _exportarXlsx() async {
    try {
      final bytes = await _service.exportarXlsx();
      await saveFile(bytes, 'transacoes.xlsx');
      await FirebaseAnalytics.instance.logEvent(name: 'exportar_xlsx');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Excel exportado com sucesso')),
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

  Future<void> _importarCsv() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['csv'],
      withData: true,
    );
    if (result == null || result.files.isEmpty) return;

    final file = result.files.first;
    if (file.bytes == null) return;

    try {
      final summary = await _service.importarCsv(
        file.bytes!.toList(),
        file.name,
      );
      await FirebaseAnalytics.instance.logEvent(name: 'importar_csv');
      if (mounted) {
        final importadas = summary['importadas'] ?? 0;
        final duplicatas = summary['duplicatas'] ?? 0;
        final erros = summary['erros'] ?? 0;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
                '$importadas importadas, $duplicatas duplicatas, $erros erros'),
          ),
        );
        if (importadas > 0) _load();
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
      await _service.deletar(t.id);
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
            icon: const Icon(Icons.upload_outlined),
            tooltip: 'Importar CSV',
            onPressed: _importarCsv,
          ),
          IconButton(
            icon: const Icon(Icons.download_outlined),
            tooltip: 'Exportar CSV',
            onPressed: _exportarCsv,
          ),
          IconButton(
            icon: const Icon(Icons.table_chart_outlined),
            tooltip: 'Exportar Excel',
            onPressed: _exportarXlsx,
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
                          controller: _scrollController,
                          padding: const EdgeInsets.only(bottom: 80),
                          itemCount: _transactions.length + (_hasMore ? 1 : 0),
                          itemBuilder: (_, i) {
                            if (i == _transactions.length) {
                              return const Padding(
                                padding: EdgeInsets.symmetric(vertical: 16),
                                child: Center(child: CircularProgressIndicator()),
                              );
                            }
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
