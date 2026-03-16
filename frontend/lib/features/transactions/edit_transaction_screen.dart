import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/models/categoria_model.dart';
import '../../core/models/conta_model.dart';
import '../../core/models/transaction_model.dart';
import '../../core/providers/auth_provider.dart';
import '../../core/services/categoria_service.dart';
import '../../core/services/conta_service.dart';
import '../../core/services/transaction_service.dart';

class EditTransactionScreen extends StatefulWidget {
  final TransactionModel transaction;

  const EditTransactionScreen({super.key, required this.transaction});

  @override
  State<EditTransactionScreen> createState() => _EditTransactionScreenState();
}

class _EditTransactionScreenState extends State<EditTransactionScreen> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _descricaoController;
  late final TextEditingController _valorController;
  late String _tipo;
  int? _categoriaId;
  int? _contaId;
  List<CategoriaModel> _categorias = [];
  List<ContaModel> _contas = [];
  bool _loading = false;

  final _service = TransactionService();
  final _categoriaService = CategoriaService();
  final _contaService = ContaService();

  @override
  void initState() {
    super.initState();
    _descricaoController =
        TextEditingController(text: widget.transaction.descricao);
    _valorController =
        TextEditingController(text: widget.transaction.valor.toStringAsFixed(2));
    _tipo = widget.transaction.tipo;
    _loadData();
  }

  Future<void> _loadData() async {
    try {
      final token = context.read<AuthProvider>().token!;
      final results = await Future.wait([
        _categoriaService.listar(token),
        _contaService.listar(token),
      ]);
      if (mounted) {
        final cats = results[0] as List<CategoriaModel>;
        final contas = results[1] as List<ContaModel>;
        setState(() {
          _categorias = cats;
          _contas = contas;
          if (widget.transaction.categoria != null) {
            _categoriaId = cats
                .where((c) => c.nome == widget.transaction.categoria)
                .map((c) => c.id)
                .firstOrNull;
          }
          if (widget.transaction.conta != null) {
            _contaId = contas
                .where((c) => c.nome == widget.transaction.conta)
                .map((c) => c.id)
                .firstOrNull;
          }
        });
      }
    } catch (_) {}
  }

  @override
  void dispose() {
    _descricaoController.dispose();
    _valorController.dispose();
    super.dispose();
  }

  Future<void> _salvar() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _loading = true);

    final token = context.read<AuthProvider>().token!;
    try {
      await _service.atualizar(
        token: token,
        id: widget.transaction.id,
        descricao: _descricaoController.text.trim(),
        valor: double.parse(_valorController.text.replaceAll(',', '.')),
        tipo: _tipo,
        categoriaId: _categoriaId,
        contaId: _contaId,
      );
      if (mounted) Navigator.pop(context, true);
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Editar transação')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              SegmentedButton<String>(
                segments: const [
                  ButtonSegment(
                      value: 'RECEITA',
                      label: Text('Receita'),
                      icon: Icon(Icons.arrow_upward)),
                  ButtonSegment(
                      value: 'DESPESA',
                      label: Text('Despesa'),
                      icon: Icon(Icons.arrow_downward)),
                ],
                selected: {_tipo},
                onSelectionChanged: (s) => setState(() => _tipo = s.first),
              ),
              const SizedBox(height: 20),
              TextFormField(
                controller: _descricaoController,
                decoration: const InputDecoration(
                  labelText: 'Descrição',
                  prefixIcon: Icon(Icons.description_outlined),
                  border: OutlineInputBorder(),
                ),
                validator: (v) =>
                    v == null || v.isEmpty ? 'Informe a descrição' : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _valorController,
                keyboardType:
                    const TextInputType.numberWithOptions(decimal: true),
                decoration: const InputDecoration(
                  labelText: 'Valor (R\$)',
                  prefixIcon: Icon(Icons.attach_money),
                  border: OutlineInputBorder(),
                ),
                validator: (v) {
                  if (v == null || v.isEmpty) return 'Informe o valor';
                  final parsed = double.tryParse(v.replaceAll(',', '.'));
                  if (parsed == null || parsed <= 0) return 'Valor inválido';
                  return null;
                },
              ),
              const SizedBox(height: 16),
              DropdownButtonFormField<int>(
                value: _categoriaId,
                decoration: const InputDecoration(
                  labelText: 'Categoria (opcional)',
                  prefixIcon: Icon(Icons.label_outline),
                  border: OutlineInputBorder(),
                ),
                items: [
                  const DropdownMenuItem(value: null, child: Text('Sem categoria')),
                  ..._categorias.map((c) => DropdownMenuItem(
                        value: c.id,
                        child: Text(c.nome),
                      )),
                ],
                onChanged: (v) => setState(() => _categoriaId = v),
              ),
              const SizedBox(height: 16),
              DropdownButtonFormField<int>(
                value: _contaId,
                decoration: const InputDecoration(
                  labelText: 'Conta (opcional)',
                  prefixIcon: Icon(Icons.account_balance_outlined),
                  border: OutlineInputBorder(),
                ),
                items: [
                  const DropdownMenuItem(value: null, child: Text('Sem conta')),
                  ..._contas.map((c) => DropdownMenuItem(
                        value: c.id,
                        child: Text(c.nome),
                      )),
                ],
                onChanged: (v) => setState(() => _contaId = v),
              ),
              const SizedBox(height: 32),
              FilledButton.icon(
                onPressed: _loading ? null : _salvar,
                icon: _loading
                    ? const SizedBox(
                        height: 18,
                        width: 18,
                        child: CircularProgressIndicator(strokeWidth: 2))
                    : const Icon(Icons.check),
                label: const Text('Salvar alterações'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
