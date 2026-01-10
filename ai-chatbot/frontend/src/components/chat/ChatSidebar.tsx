import { Plus, MessageSquare, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { ScrollArea } from '@/components/ui/scroll-area';
import type { SessionResponse } from '@/types/chat';
import { cn } from '@/lib/utils';

interface ChatSidebarProps {
  sessions: SessionResponse[];
  currentSessionId: string | null;
  onSelectSession: (sessionId: string) => void;
  onNewChat: () => void;
  onDeleteSession: (sessionId: string) => void;
}

export function ChatSidebar({
  sessions,
  currentSessionId,
  onSelectSession,
  onNewChat,
  onDeleteSession,
}: ChatSidebarProps) {
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
    });
  };

  return (
    <div className="flex h-full w-64 flex-col border-r bg-slate-50">
      <div className="p-4">
        <Button onClick={onNewChat} className="w-full gap-2">
          <Plus className="h-4 w-4" />
          New Chat
        </Button>
      </div>
      <ScrollArea className="flex-1 px-2">
        <div className="space-y-1 pb-4">
          {sessions.map((session) => (
            <div
              key={session.id}
              className={cn(
                'group flex items-center gap-2 rounded-lg px-3 py-2 text-sm transition-colors hover:bg-slate-200 cursor-pointer',
                currentSessionId === session.id && 'bg-slate-200'
              )}
              onClick={() => onSelectSession(session.id)}
            >
              <MessageSquare className="h-4 w-4 shrink-0 text-slate-500" />
              <div className="flex-1 truncate">
                <div className="truncate font-medium">{session.title}</div>
                <div className="text-xs text-slate-500">
                  {formatDate(session.createdAt)} · {session.messageCount} messages
                </div>
              </div>
              <Button
                variant="ghost"
                size="icon"
                className="h-6 w-6 opacity-0 group-hover:opacity-100"
                onClick={(e) => {
                  e.stopPropagation();
                  onDeleteSession(session.id);
                }}
              >
                <Trash2 className="h-3 w-3 text-slate-500 hover:text-red-500" />
              </Button>
            </div>
          ))}
          {sessions.length === 0 && (
            <div className="px-3 py-8 text-center text-sm text-slate-500">
              No chat sessions yet. Start a new chat!
            </div>
          )}
        </div>
      </ScrollArea>
    </div>
  );
}
