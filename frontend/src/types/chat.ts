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

// Security Configuration Types
export type SecurityConfigType = 'SSL_CONFIG' | 'DATABASE_CREDENTIALS';

export interface SslConfig {
  id: string;
  name: string;
  description?: string;
  trustStorePath?: string;
  trustStorePassword?: string;
  keyStorePath?: string;
  keyStorePassword?: string;
  verifyHostname: boolean;
  createdAt: string;
}

export interface DatabaseCredentials {
  id: string;
  name: string;
  description?: string;
  jdbcUrl: string;
  username: string;
  password: string;
  driverClassName?: string;
  createdAt: string;
}

export interface SecurityConfigResponse {
  id: string;
  name: string;
  description?: string;
  type: SecurityConfigType;
  createdAt: string;
}

export interface CreateSslConfigRequest {
  name: string;
  description?: string;
  trustStorePath?: string;
  trustStorePassword?: string;
  keyStorePath?: string;
  keyStorePassword?: string;
  verifyHostname?: boolean;
}

export interface CreateDatabaseCredentialsRequest {
  name: string;
  description?: string;
  jdbcUrl: string;
  username: string;
  password: string;
  driverClassName?: string;
}

// Tool and Toolset Types
export type ToolType = 'HTTP_ENDPOINT' | 'JDBC_QUERY';

export interface ToolParameter {
  name: string;
  description?: string;
  type: string;
  required: boolean;
  defaultValue?: string;
}

export interface HttpEndpointConfig {
  url: string;
  method: string;
  headers: Record<string, string>;
  bodyTemplate?: string;
  sslConfigId?: string;
}

export interface JdbcQueryConfig {
  queryTemplate: string;
  databaseCredentialsId: string;
}

export interface Tool {
  id: string;
  name: string;
  description?: string;
  type: ToolType;
  parameters: ToolParameter[];
  httpConfig?: HttpEndpointConfig;
  jdbcConfig?: JdbcQueryConfig;
  createdAt: string;
}

export interface Toolset {
  id: string;
  name: string;
  description?: string;
  tools: Tool[];
  createdAt: string;
}

export interface ToolsetResponse {
  id: string;
  name: string;
  description?: string;
  toolCount: number;
  createdAt: string;
}

export interface CreateToolsetRequest {
  name: string;
  description?: string;
}

export interface CreateToolRequest {
  name: string;
  description?: string;
  type: ToolType;
  parameters?: ToolParameter[];
  httpConfig?: HttpEndpointConfig;
  jdbcConfig?: JdbcQueryConfig;
}
