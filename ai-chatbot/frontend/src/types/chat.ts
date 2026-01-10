export interface ChatSession {
  id: string;
  title: string;
  createdAt: string;
  messages: ChatMessage[];
}

export interface ChatMessage {
  id: string;
  sessionId: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  timestamp: string;
}

export interface SessionResponse {
  id: string;
  title: string;
  createdAt: string;
  messageCount: number;
}

export interface SessionDetailResponse {
  id: string;
  title: string;
  createdAt: string;
  messages: ChatMessage[];
}

export interface MessageResponse {
  userMessage: ChatMessage;
  assistantMessage: ChatMessage;
}

export interface CreateSessionRequest {
  title?: string;
}

export interface SendMessageRequest {
  content: string;
}
