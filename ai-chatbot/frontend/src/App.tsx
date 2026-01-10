import { useState } from 'react';
import { ChatPage } from './components/chat/ChatPage';
import { DocumentsPage } from './components/documents/DocumentsPage';
import { SecurityConfigPage } from './components/security/SecurityConfigPage';
import { ToolsetsPage } from './components/toolsets/ToolsetsPage';
import { MessageSquare, FileText, Shield, Wrench } from 'lucide-react';
import { Button } from '@/components/ui/button';

type Page = 'chat' | 'documents' | 'security' | 'toolsets';

function App() {
  const [currentPage, setCurrentPage] = useState<Page>('chat');

  const renderPage = () => {
    switch (currentPage) {
      case 'chat':
        return <ChatPage />;
      case 'documents':
        return <DocumentsPage />;
      case 'security':
        return <SecurityConfigPage />;
      case 'toolsets':
        return <ToolsetsPage />;
      default:
        return <ChatPage />;
    }
  };

  return (
    <div className="flex h-screen">
      <nav className="flex w-16 flex-col items-center gap-2 border-r bg-slate-900 py-4">
        <Button
          variant={currentPage === 'chat' ? 'secondary' : 'ghost'}
          size="icon"
          className={currentPage === 'chat' ? 'bg-slate-700' : 'text-slate-400 hover:text-white hover:bg-slate-800'}
          onClick={() => setCurrentPage('chat')}
          title="Chat"
        >
          <MessageSquare className="h-5 w-5" />
        </Button>
        <Button
          variant={currentPage === 'documents' ? 'secondary' : 'ghost'}
          size="icon"
          className={currentPage === 'documents' ? 'bg-slate-700' : 'text-slate-400 hover:text-white hover:bg-slate-800'}
          onClick={() => setCurrentPage('documents')}
          title="Documents"
        >
          <FileText className="h-5 w-5" />
        </Button>
        <Button
          variant={currentPage === 'toolsets' ? 'secondary' : 'ghost'}
          size="icon"
          className={currentPage === 'toolsets' ? 'bg-slate-700' : 'text-slate-400 hover:text-white hover:bg-slate-800'}
          onClick={() => setCurrentPage('toolsets')}
          title="Toolsets"
        >
          <Wrench className="h-5 w-5" />
        </Button>
        <Button
          variant={currentPage === 'security' ? 'secondary' : 'ghost'}
          size="icon"
          className={currentPage === 'security' ? 'bg-slate-700' : 'text-slate-400 hover:text-white hover:bg-slate-800'}
          onClick={() => setCurrentPage('security')}
          title="Security"
        >
          <Shield className="h-5 w-5" />
        </Button>
      </nav>
      <main className="flex-1">
        {renderPage()}
      </main>
    </div>
  );
}

export default App
