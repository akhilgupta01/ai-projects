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
  SslConfig,
  DatabaseCredentials,
  SecurityConfigResponse,
  CreateSslConfigRequest,
  CreateDatabaseCredentialsRequest,
  Toolset,
  ToolsetResponse,
  Tool,
  CreateToolsetRequest,
  CreateToolRequest,
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

// Security Configuration APIs
export async function getAllSecurityConfigs(): Promise<SecurityConfigResponse[]> {
  const response = await fetch(`${API_URL}/api/security-configs`);
  return handleResponse<SecurityConfigResponse[]>(response);
}

export async function createSslConfig(request: CreateSslConfigRequest): Promise<SslConfig> {
  const response = await fetch(`${API_URL}/api/security-configs/ssl`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });
  return handleResponse<SslConfig>(response);
}

export async function getAllSslConfigs(): Promise<SslConfig[]> {
  const response = await fetch(`${API_URL}/api/security-configs/ssl`);
  return handleResponse<SslConfig[]>(response);
}

export async function getSslConfig(id: string): Promise<SslConfig> {
  const response = await fetch(`${API_URL}/api/security-configs/ssl/${id}`);
  return handleResponse<SslConfig>(response);
}

export async function deleteSslConfig(id: string): Promise<void> {
  const response = await fetch(`${API_URL}/api/security-configs/ssl/${id}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    throw new Error(`Failed to delete SSL config: ${response.status}`);
  }
}

export async function createDatabaseCredentials(request: CreateDatabaseCredentialsRequest): Promise<DatabaseCredentials> {
  const response = await fetch(`${API_URL}/api/security-configs/database`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });
  return handleResponse<DatabaseCredentials>(response);
}

export async function getAllDatabaseCredentials(): Promise<DatabaseCredentials[]> {
  const response = await fetch(`${API_URL}/api/security-configs/database`);
  return handleResponse<DatabaseCredentials[]>(response);
}

export async function getDatabaseCredentials(id: string): Promise<DatabaseCredentials> {
  const response = await fetch(`${API_URL}/api/security-configs/database/${id}`);
  return handleResponse<DatabaseCredentials>(response);
}

export async function deleteDatabaseCredentials(id: string): Promise<void> {
  const response = await fetch(`${API_URL}/api/security-configs/database/${id}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    throw new Error(`Failed to delete database credentials: ${response.status}`);
  }
}

// Toolset APIs
export async function createToolset(request: CreateToolsetRequest): Promise<Toolset> {
  const response = await fetch(`${API_URL}/api/toolsets`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });
  return handleResponse<Toolset>(response);
}

export async function getAllToolsets(): Promise<ToolsetResponse[]> {
  const response = await fetch(`${API_URL}/api/toolsets`);
  return handleResponse<ToolsetResponse[]>(response);
}

export async function getToolset(id: string): Promise<Toolset> {
  const response = await fetch(`${API_URL}/api/toolsets/${id}`);
  return handleResponse<Toolset>(response);
}

export async function updateToolset(id: string, request: CreateToolsetRequest): Promise<Toolset> {
  const response = await fetch(`${API_URL}/api/toolsets/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });
  return handleResponse<Toolset>(response);
}

export async function deleteToolset(id: string): Promise<void> {
  const response = await fetch(`${API_URL}/api/toolsets/${id}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    throw new Error(`Failed to delete toolset: ${response.status}`);
  }
}

export async function addToolToToolset(toolsetId: string, request: CreateToolRequest): Promise<Tool> {
  const response = await fetch(`${API_URL}/api/toolsets/${toolsetId}/tools`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });
  return handleResponse<Tool>(response);
}

export async function getToolsInToolset(toolsetId: string): Promise<Tool[]> {
  const response = await fetch(`${API_URL}/api/toolsets/${toolsetId}/tools`);
  return handleResponse<Tool[]>(response);
}

export async function getTool(toolsetId: string, toolId: string): Promise<Tool> {
  const response = await fetch(`${API_URL}/api/toolsets/${toolsetId}/tools/${toolId}`);
  return handleResponse<Tool>(response);
}

export async function deleteToolFromToolset(toolsetId: string, toolId: string): Promise<void> {
  const response = await fetch(`${API_URL}/api/toolsets/${toolsetId}/tools/${toolId}`, {
    method: 'DELETE',
  });
  if (!response.ok) {
    throw new Error(`Failed to delete tool: ${response.status}`);
  }
}
