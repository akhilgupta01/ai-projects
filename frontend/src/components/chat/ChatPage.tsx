import { useState, useEffect, useRef } from "react";
import { Bot } from "lucide-react";
import { ChatSidebar } from "./ChatSidebar";
import { ChatMessage } from "./ChatMessage";
import { ChatInput, type DocumentAttachment } from "./ChatInput";
import { ScrollArea } from "@/components/ui/scroll-area";
import * as api from "@/services/api";
import type {
  SessionResponse,
  ChatMessage as ChatMessageType,
} from "@/types/chat";

export function ChatPage() {
  const [sessions, setSessions] = useState<SessionResponse[]>([]);
  const [currentSessionId, setCurrentSessionId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessageType[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    loadSessions();
  }, []);

  const loadSessions = async () => {
    try {
      const data = await api.getAllSessions();
      setSessions(data);
    } catch (error) {
      console.error("Failed to load sessions:", error);
    }
  };

  const loadSession = async (sessionId: string) => {
    setIsLoading(true);
    try {
      const data = await api.getSession(sessionId);
      setMessages(data.messages);
      setCurrentSessionId(sessionId);
    } catch (error) {
      console.error("Failed to load session:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleNewChat = async () => {
    try {
      const session = await api.createSession();
      await loadSessions();
      setCurrentSessionId(session.id);
      setMessages([]);
    } catch (error) {
      console.error("Failed to create session:", error);
    }
  };

  const handleSelectSession = (sessionId: string) => {
    if (sessionId !== currentSessionId) {
      loadSession(sessionId);
    }
  };

  const handleDeleteSession = async (sessionId: string) => {
    try {
      await api.deleteSession(sessionId);
      await loadSessions();
      if (currentSessionId === sessionId) {
        setCurrentSessionId(null);
        setMessages([]);
      }
    } catch (error) {
      console.error("Failed to delete session:", error);
    }
  };

  const handleSendMessage = async (
    content: string,
    attachments?: DocumentAttachment[],
  ) => {
    // Upload documents first if any attachments
    if (attachments && attachments.length > 0) {
      try {
        for (const attachment of attachments) {
          await api.uploadDocument({
            name: attachment.name,
            content: attachment.content,
            contentType: attachment.contentType,
            tags: ["chat-attachment"],
          });
        }
      } catch (error) {
        console.error("Failed to upload documents:", error);
      }
    }

    if (!currentSessionId) {
      const session = await api.createSession();
      await loadSessions();
      setCurrentSessionId(session.id);
      await sendMessageToSession(session.id, content);
    } else {
      await sendMessageToSession(currentSessionId, content);
    }
  };

  const sendMessageToSession = async (sessionId: string, content: string) => {
    setIsSending(true);
    try {
      const response = await api.sendMessage(sessionId, { content });
      setMessages((prev) => [
        ...prev,
        response.userMessage,
        response.assistantMessage,
      ]);
      await loadSessions();
    } catch (error) {
      console.error("Failed to send message:", error);
    } finally {
      setIsSending(false);
    }
  };

  return (
    <div className="flex h-screen bg-white">
      <ChatSidebar
        sessions={sessions}
        currentSessionId={currentSessionId}
        onSelectSession={handleSelectSession}
        onNewChat={handleNewChat}
        onDeleteSession={handleDeleteSession}
      />
      <div className="flex flex-1 flex-col">
        <header className="flex h-14 items-center border-b px-6">
          <div className="flex items-center gap-2">
            <Bot className="h-6 w-6 text-emerald-600" />
            <h1 className="text-lg font-semibold">AI Operational Support</h1>
          </div>
        </header>
        <div className="flex-1 overflow-hidden">
          {isLoading ? (
            <div className="flex h-full items-center justify-center">
              <div className="text-slate-500">Loading...</div>
            </div>
          ) : messages.length === 0 ? (
            <div className="flex h-full flex-col items-center justify-center gap-4 p-8">
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-emerald-100">
                <Bot className="h-8 w-8 text-emerald-600" />
              </div>
              <h2 className="text-xl font-semibold text-slate-800">
                AI Operational Support Assistant
              </h2>
              <p className="max-w-md text-center text-slate-600">
                I'm here to help you with operational support questions, system
                status inquiries, troubleshooting, and more. Start a
                conversation by typing a message below.
              </p>
              <div className="mt-4 grid gap-2 text-sm text-slate-500">
                <div className="rounded-lg border bg-slate-50 px-4 py-2">
                  "What's the current system status?"
                </div>
                <div className="rounded-lg border bg-slate-50 px-4 py-2">
                  "I'm experiencing an error with the service"
                </div>
                <div className="rounded-lg border bg-slate-50 px-4 py-2">
                  "How do I restart the application?"
                </div>
              </div>
            </div>
          ) : (
            <ScrollArea className="h-full">
              <div className="mx-auto max-w-3xl">
                {messages.map((message) => (
                  <ChatMessage key={message.id} message={message} />
                ))}
                <div ref={messagesEndRef} />
              </div>
            </ScrollArea>
          )}
        </div>
        <ChatInput onSendMessage={handleSendMessage} disabled={isSending} />
      </div>
    </div>
  );
}
