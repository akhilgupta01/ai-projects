import { useAuth } from '../../context/AuthContext';
import { Button } from '@/components/ui/button';
import { Shield } from 'lucide-react';

export const LoginPage = () => {
  const { login } = useAuth();

  return (
    <div className="flex h-screen items-center justify-center bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900">
      <div className="w-full max-w-md rounded-lg bg-slate-800 p-8 shadow-2xl border border-slate-700">
        <div className="flex flex-col items-center space-y-6">
          <div className="rounded-full bg-slate-700 p-4">
            <Shield className="h-12 w-12 text-blue-400" />
          </div>
          <div className="text-center">
            <h1 className="text-3xl font-bold text-white mb-2">AI Chatbot</h1>
            <p className="text-slate-400">
              Secure authentication required to access the application
            </p>
          </div>
          <Button
            onClick={() => login()}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white py-6 text-lg"
            size="lg"
          >
            Sign In with OAuth
          </Button>
          <p className="text-sm text-slate-500 text-center">
            By signing in, you agree to our Terms of Service and Privacy Policy
          </p>
        </div>
      </div>
    </div>
  );
};
