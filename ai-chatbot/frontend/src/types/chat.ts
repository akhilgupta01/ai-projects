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

export interface KnowledgeDocument {
  id: string;
  name: string;
  content: string;
  contentType: string;
  size: number;
  uploadedAt: string;
  tags: string[];
}

export interface DocumentResponse {
  id: string;
  name: string;
  contentType: string;
  size: number;
  uploadedAt: string;
  tags: string[];
}

export interface UploadDocumentRequest {
  name: string;
  content: string;
  contentType: string;
  tags?: string[];
}

export interface SearchDocumentsRequest {
  query: string;
  tags?: string[];
}
