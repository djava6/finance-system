import 'package:firebase_analytics/firebase_analytics.dart';
import 'package:flutter/material.dart';
import '../../core/models/categoria_model.dart';
import '../../core/models/conta_model.dart';
import '../../core/services/categoria_service.dart';
import '../../core/services/conta_service.dart';
import '../../core/services/transaction_service.dart';

class AddTransactionScreen extends StatefulWidget {
  const AddTransactionScreen({super.key});

  @override
  State<AddTransactionScreen> createState() => _AddTransactionScreenState();
}

class _AddTransactionScreenState extends State<AddTransactionScreen> {
  final _formKey = GlobalKey<FormState>();
  final _descricaoController = TextEditingController();
  final _valorController = TextEditingController();
  String _tipo = 'DESPESA';
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
    _loadData();
  }

  Future<void> _loadData() async {
    try {
      final results = await Future.wait([
        _categoriaService.listar(),
        _contaService.listar(),
      ]);
      if (mounted) {
        setState(() {
          _categorias = results[0] as List<CategoriaModel>;
          _contas = results[1] as List<ContaModel>;
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

    try {
      await _service.criar(
        descricao: _descricaoController.text.trim(),
        valor: double.parse(_valorController.text.replaceAll(',', '.')),
        tipo: _tipo,
        categoriaId: _categoriaId,
        contaId: _contaId,
      );
      await FirebaseAnalytics.instance.logEvent(
        name: 'criar_transacao',
        parameters: {'tipo': _tipo.toLowerCase()},
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
      appBar: AppBar(title: const Text('Nova transação')),
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
                  final parsed =
                      double.tryParse(v.replaceAll(',', '.'));
                  if (parsed == null || parsed <= 0) {
                    return 'Valor inválido';
                  }
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
                label: const Text('Salvar'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}