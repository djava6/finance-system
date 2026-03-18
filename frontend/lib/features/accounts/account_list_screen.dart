import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../core/models/conta_model.dart';
import '../../core/services/conta_service.dart';

class AccountListScreen extends StatefulWidget {
  const AccountListScreen({super.key});

  @override
  State<AccountListScreen> createState() => _AccountListScreenState();
}

class _AccountListScreenState extends State<AccountListScreen> {
  final _service = ContaService();
  List<ContaModel> _contas = [];
  bool _loading = true;

  final _currency = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      _contas = await _service.listar();
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

  Future<void> _showAddEditDialog({ContaModel? conta}) async {
    final nomeController = TextEditingController(text: conta?.nome ?? '');
    final saldoController = TextEditingController(
        text: conta != null ? conta.saldo.toStringAsFixed(2) : '');
    final formKey = GlobalKey<FormState>();

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(conta == null ? 'Nova conta' : 'Editar conta'),
        content: Form(
          key: formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                controller: nomeController,
                decoration: const InputDecoration(
                  labelText: 'Nome',
                  border: OutlineInputBorder(),
                ),
                validator: (v) =>
                    v == null || v.isEmpty ? 'Informe o nome' : null,
                autofocus: true,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: saldoController,
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                decoration: const InputDecoration(
                  labelText: 'Saldo inicial (R\$)',
                  border: OutlineInputBorder(),
                ),
                validator: (v) {
                  if (v == null || v.isEmpty) return 'Informe o saldo';
                  if (double.tryParse(v.replaceAll(',', '.')) == null) {
                    return 'Valor inválido';
                  }
                  return null;
                },
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('Cancelar')),
          FilledButton(
              onPressed: () {
                if (formKey.currentState!.validate()) {
                  Navigator.pop(ctx, true);
                }
              },
              child: const Text('Salvar')),
        ],
      ),
    );

    if (confirmed == true) {
      final nome = nomeController.text.trim();
      final saldo =
          double.parse(saldoController.text.replaceAll(',', '.'));
      try {
        if (conta == null) {
          await _service.criar(nome, saldo);
        } else {
          await _service.atualizar(conta.id, nome, saldo);
        }
        await _load();
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
                content:
                    Text(e.toString().replaceFirst('Exception: ', ''))),
          );
        }
      }
    }
  }

  Future<void> _deletar(ContaModel conta) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Excluir conta'),
        content: Text('Excluir "${conta.nome}"?'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('Cancelar')),
          FilledButton(
              style: FilledButton.styleFrom(backgroundColor: Colors.red),
              onPressed: () => Navigator.pop(ctx, true),
              child: const Text('Excluir')),
        ],
      ),
    );
    if (confirmed == true) {
      try {
        await _service.deletar(conta.id);
        await _load();
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
                content:
                    Text(e.toString().replaceFirst('Exception: ', ''))),
          );
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final totalSaldo =
        _contas.fold<double>(0, (sum, c) => sum + c.saldo);

    return Scaffold(
      appBar: AppBar(title: const Text('Contas')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : Column(
              children: [
                if (_contas.isNotEmpty)
                  Container(
                    width: double.infinity,
                    margin: const EdgeInsets.all(16),
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.primaryContainer,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Column(
                      children: [
                        const Text('Patrimônio total',
                            style: TextStyle(fontSize: 13)),
                        const SizedBox(height: 4),
                        Text(
                          _currency.format(totalSaldo),
                          style: TextStyle(
                            fontSize: 22,
                            fontWeight: FontWeight.bold,
                            color: totalSaldo >= 0
                                ? Colors.green.shade700
                                : Colors.red,
                          ),
                        ),
                      ],
                    ),
                  ),
                Expanded(
                  child: _contas.isEmpty
                      ? const Center(
                          child: Text('Nenhuma conta cadastrada.'))
                      : RefreshIndicator(
                          onRefresh: _load,
                          child: ListView.separated(
                            itemCount: _contas.length,
                            separatorBuilder: (_, __) =>
                                const Divider(height: 1),
                            itemBuilder: (_, i) {
                              final c = _contas[i];
                              return ListTile(
                                leading: const CircleAvatar(
                                  child:
                                      Icon(Icons.account_balance_outlined),
                                ),
                                title: Text(c.nome),
                                subtitle: Text(_currency.format(c.saldo),
                                    style: TextStyle(
                                      color: c.saldo >= 0
                                          ? Colors.green
                                          : Colors.red,
                                      fontWeight: FontWeight.bold,
                                    )),
                                trailing: Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    IconButton(
                                      icon: const Icon(Icons.edit_outlined),
                                      onPressed: () =>
                                          _showAddEditDialog(conta: c),
                                    ),
                                    IconButton(
                                      icon: const Icon(
                                          Icons.delete_outline,
                                          color: Colors.red),
                                      onPressed: () => _deletar(c),
                                    ),
                                  ],
                                ),
                              );
                            },
                          ),
                        ),
                ),
              ],
            ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showAddEditDialog(),
        child: const Icon(Icons.add),
      ),
    );
  }
}
