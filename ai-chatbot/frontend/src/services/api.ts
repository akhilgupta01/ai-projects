import type {
  ChatSession,
  SessionResponse,
  SessionDetailResponse,
  MessageResponse,
  CreateSessionRequest,
  SendMessageRequest,
  KnowledgeDocument,
  DocumentResponse,
  UploadDocumentRequest,
  SearchDocumentsRequest,
} from '../types/chat';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const error = await response.text();
    throw new Error(error || `HTTP error! status: ${response.status}`);
  }
  return response.json();
}

export async function createSession(request?: CreateSessionRequest): Promise<ChatSession> {
  const response = await fetch(`${API_URL}/api/sessions`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request || {}),
  });
  return handleResponse<ChatSession>(response);
}

export async function getAllSessions(): Promise<SessionResponse[]> {
  const response = await fetch(`${API_URL}/api/sessions`);
  return handleResponse<SessionResponse[]>(response);
}

export async function getSession(sessionId: string): Promise<SessionDetailResponse> {
  const response = await fetch(`${API_URL}/api/sessions/${sessionId}`);
  return handleResponse<SessionDetailResponse>(response);
}

export async function deleteSession(sessionId: string): Promise<void> {
  const response = await fetch(`${API_URL}/api/sessions/${sessionId}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    throw new Error(`Failed to delete session: ${response.status}`);
  }
}

export async function sendMessage(
  sessionId: string,
  request: SendMessageRequest
): Promise<MessageResponse> {
  const response = await fetch(`${API_URL}/api/sessions/${sessionId}/messages`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });
  return handleResponse<MessageResponse>(response);
}

export async function uploadDocument(request: UploadDocumentRequest): Promise<KnowledgeDocument> {
  const response = await fetch(`${API_URL}/api/documents`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });
  return handleResponse<KnowledgeDocument>(response);
}

export async function getAllDocuments(): Promise<DocumentResponse[]> {
  const response = await fetch(`${API_URL}/api/documents`);
  return handleResponse<DocumentResponse[]>(response);
}

export async function getDocument(documentId: string): Promise<KnowledgeDocument> {
  const response = await fetch(`${API_URL}/api/documents/${documentId}`);
  return handleResponse<KnowledgeDocument>(response);
}

export async function deleteDocument(documentId: string): Promise<void> {
  const response = await fetch(`${API_URL}/api/documents/${documentId}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    throw new Error(`Failed to delete document: ${response.status}`);
  }
}

export async function searchDocuments(request: SearchDocumentsRequest): Promise<DocumentResponse[]> {
  const response = await fetch(`${API_URL}/api/documents/search`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });
  return handleResponse<DocumentResponse[]>(response);
}
