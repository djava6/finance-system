import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/models/user_profile_model.dart';
import '../../core/providers/auth_provider.dart';
import '../../core/services/user_service.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  final _service = UserService();
  final _formKey = GlobalKey<FormState>();
  final _nomeController = TextEditingController();

  UserProfileModel? _profile;
  bool _loading = true;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _nomeController.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final token = context.read<AuthProvider>().token!;
      _profile = await _service.getProfile(token);
      _nomeController.text = _profile!.nome;
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

  Future<void> _salvar() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _saving = true);

    try {
      final token = context.read<AuthProvider>().token!;
      final updated = await _service.updateProfile(
          token, _nomeController.text.trim(), null);

      // Update nome in AuthProvider
      if (mounted) {
        context.read<AuthProvider>().updateNome(updated.nome);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Perfil atualizado com sucesso!')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))),
        );
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Meu Perfil')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    CircleAvatar(
                      radius: 40,
                      child: Text(
                        (_profile?.nome.isNotEmpty == true)
                            ? _profile!.nome[0].toUpperCase()
                            : '?',
                        style: const TextStyle(fontSize: 32),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      _profile?.email ?? '',
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                            color: Colors.grey,
                          ),
                    ),
                    const SizedBox(height: 32),
                    TextFormField(
                      controller: _nomeController,
                      decoration: const InputDecoration(
                        labelText: 'Nome',
                        prefixIcon: Icon(Icons.person_outline),
                        border: OutlineInputBorder(),
                      ),
                      validator: (v) =>
                          v == null || v.isEmpty ? 'Informe o nome' : null,
                    ),
                    const SizedBox(height: 32),
                    FilledButton.icon(
                      onPressed: _saving ? null : _salvar,
                      icon: _saving
                          ? const SizedBox(
                              height: 18,
                              width: 18,
                              child:
                                  CircularProgressIndicator(strokeWidth: 2))
                          : const Icon(Icons.save_outlined),
                      label: const Text('Salvar alterações'),
                    ),
                  ],
                ),
              ),
            ),
    );
  }
}
