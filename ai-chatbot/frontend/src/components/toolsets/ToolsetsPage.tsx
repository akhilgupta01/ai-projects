import { useState, useEffect } from 'react';
import { Wrench, Globe, Database, Trash2, Plus, ChevronRight, ArrowLeft } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import * as api from '@/services/api';
import type { ToolsetResponse, Toolset, ToolType, SslConfig, DatabaseCredentials } from '@/types/chat';

export function ToolsetsPage() {
  const [toolsets, setToolsets] = useState<ToolsetResponse[]>([]);
  const [selectedToolset, setSelectedToolset] = useState<Toolset | null>(null);
  const [sslConfigs, setSslConfigs] = useState<SslConfig[]>([]);
  const [dbCredentials, setDbCredentials] = useState<DatabaseCredentials[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const [toolsetDialogOpen, setToolsetDialogOpen] = useState(false);
  const [toolDialogOpen, setToolDialogOpen] = useState(false);

  const [newToolsetName, setNewToolsetName] = useState('');
  const [newToolsetDescription, setNewToolsetDescription] = useState('');

  const [newToolName, setNewToolName] = useState('');
  const [newToolDescription, setNewToolDescription] = useState('');
  const [newToolType, setNewToolType] = useState<ToolType>('HTTP_ENDPOINT');

  const [newHttpUrl, setNewHttpUrl] = useState('');
  const [newHttpMethod, setNewHttpMethod] = useState('GET');
  const [newHttpHeaders, setNewHttpHeaders] = useState('');
  const [newHttpBodyTemplate, setNewHttpBodyTemplate] = useState('');
  const [newHttpSslConfigId, setNewHttpSslConfigId] = useState<string | undefined>();

  const [newJdbcQuery, setNewJdbcQuery] = useState('');
  const [newJdbcDbCredentialsId, setNewJdbcDbCredentialsId] = useState('');

  useEffect(() => {
    loadToolsets();
    loadSecurityConfigs();
  }, []);

  const loadToolsets = async () => {
    setIsLoading(true);
    try {
      const data = await api.getAllToolsets();
      setToolsets(data);
    } catch (error) {
      console.error('Failed to load toolsets:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const loadSecurityConfigs = async () => {
    try {
      const [ssl, db] = await Promise.all([
        api.getAllSslConfigs(),
        api.getAllDatabaseCredentials(),
      ]);
      setSslConfigs(ssl);
      setDbCredentials(db);
    } catch (error) {
      console.error('Failed to load security configs:', error);
    }
  };

  const loadToolsetDetails = async (id: string) => {
    try {
      const toolset = await api.getToolset(id);
      setSelectedToolset(toolset);
    } catch (error) {
      console.error('Failed to load toolset details:', error);
    }
  };

  const handleCreateToolset = async () => {
    if (!newToolsetName.trim()) return;

    try {
      await api.createToolset({
        name: newToolsetName,
        description: newToolsetDescription || undefined,
      });

      setNewToolsetName('');
      setNewToolsetDescription('');
      setToolsetDialogOpen(false);
      loadToolsets();
    } catch (error) {
      console.error('Failed to create toolset:', error);
    }
  };

  const handleDeleteToolset = async (id: string) => {
    try {
      await api.deleteToolset(id);
      if (selectedToolset?.id === id) {
        setSelectedToolset(null);
      }
      loadToolsets();
    } catch (error) {
      console.error('Failed to delete toolset:', error);
    }
  };

  const handleCreateTool = async () => {
    if (!selectedToolset || !newToolName.trim()) return;

    try {
      const headersObj: Record<string, string> = {};
      if (newHttpHeaders.trim()) {
        newHttpHeaders.split('\n').forEach((line) => {
          const [key, ...valueParts] = line.split(':');
          if (key && valueParts.length > 0) {
            headersObj[key.trim()] = valueParts.join(':').trim();
          }
        });
      }

      await api.addToolToToolset(selectedToolset.id, {
        name: newToolName,
        description: newToolDescription || undefined,
        type: newToolType,
        parameters: [],
        httpConfig: newToolType === 'HTTP_ENDPOINT' ? {
          url: newHttpUrl,
          method: newHttpMethod,
          headers: headersObj,
          bodyTemplate: newHttpBodyTemplate || undefined,
          sslConfigId: newHttpSslConfigId || undefined,
        } : undefined,
        jdbcConfig: newToolType === 'JDBC_QUERY' ? {
          queryTemplate: newJdbcQuery,
          databaseCredentialsId: newJdbcDbCredentialsId,
        } : undefined,
      });

      resetToolForm();
      setToolDialogOpen(false);
      loadToolsetDetails(selectedToolset.id);
    } catch (error) {
      console.error('Failed to create tool:', error);
    }
  };

  const handleDeleteTool = async (toolId: string) => {
    if (!selectedToolset) return;

    try {
      await api.deleteToolFromToolset(selectedToolset.id, toolId);
      loadToolsetDetails(selectedToolset.id);
    } catch (error) {
      console.error('Failed to delete tool:', error);
    }
  };

  const resetToolForm = () => {
    setNewToolName('');
    setNewToolDescription('');
    setNewToolType('HTTP_ENDPOINT');
    setNewHttpUrl('');
    setNewHttpMethod('GET');
    setNewHttpHeaders('');
    setNewHttpBodyTemplate('');
    setNewHttpSslConfigId(undefined);
    setNewJdbcQuery('');
    setNewJdbcDbCredentialsId('');
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  };

  if (selectedToolset) {
    return (
      <div className="flex h-screen flex-col bg-slate-50">
        <header className="flex h-14 items-center justify-between border-b bg-white px-6">
          <div className="flex items-center gap-2">
            <Button variant="ghost" size="icon" onClick={() => setSelectedToolset(null)}>
              <ArrowLeft className="h-5 w-5" />
            </Button>
            <Wrench className="h-6 w-6 text-purple-600" />
            <h1 className="text-lg font-semibold">{selectedToolset.name}</h1>
            <Badge variant="secondary">{selectedToolset.tools.length} tools</Badge>
          </div>
          <Dialog open={toolDialogOpen} onOpenChange={setToolDialogOpen}>
            <DialogTrigger asChild>
              <Button className="gap-2">
                <Plus className="h-4 w-4" />
                Add Tool
              </Button>
            </DialogTrigger>
            <DialogContent className="sm:max-w-lg max-h-[90vh] overflow-y-auto">
              <DialogHeader>
                <DialogTitle>Add Tool to Toolset</DialogTitle>
              </DialogHeader>
              <div className="space-y-4 py-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium">Tool Name *</label>
                  <Input
                    value={newToolName}
                    onChange={(e) => setNewToolName(e.target.value)}
                    placeholder="Enter tool name"
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Description</label>
                  <Textarea
                    value={newToolDescription}
                    onChange={(e) => setNewToolDescription(e.target.value)}
                    placeholder="Enter tool description"
                    rows={2}
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Tool Type *</label>
                  <Tabs value={newToolType} onValueChange={(v) => setNewToolType(v as ToolType)}>
                    <TabsList className="w-full">
                      <TabsTrigger value="HTTP_ENDPOINT" className="flex-1 gap-2">
                        <Globe className="h-4 w-4" />
                        HTTP Endpoint
                      </TabsTrigger>
                      <TabsTrigger value="JDBC_QUERY" className="flex-1 gap-2">
                        <Database className="h-4 w-4" />
                        JDBC Query
                      </TabsTrigger>
                    </TabsList>

                    <TabsContent value="HTTP_ENDPOINT" className="space-y-4 mt-4">
                      <div className="space-y-2">
                        <label className="text-sm font-medium">URL *</label>
                        <Input
                          value={newHttpUrl}
                          onChange={(e) => setNewHttpUrl(e.target.value)}
                          placeholder="https://api.example.com/endpoint"
                        />
                      </div>
                      <div className="space-y-2">
                        <label className="text-sm font-medium">HTTP Method</label>
                        <Select value={newHttpMethod} onValueChange={setNewHttpMethod}>
                          <SelectTrigger>
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="GET">GET</SelectItem>
                            <SelectItem value="POST">POST</SelectItem>
                            <SelectItem value="PUT">PUT</SelectItem>
                            <SelectItem value="DELETE">DELETE</SelectItem>
                            <SelectItem value="PATCH">PATCH</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                      <div className="space-y-2">
                        <label className="text-sm font-medium">Headers (one per line, key: value)</label>
                        <Textarea
                          value={newHttpHeaders}
                          onChange={(e) => setNewHttpHeaders(e.target.value)}
                          placeholder="Content-Type: application/json&#10;Authorization: Bearer token"
                          rows={3}
                        />
                      </div>
                      <div className="space-y-2">
                        <label className="text-sm font-medium">Body Template</label>
                        <Textarea
                          value={newHttpBodyTemplate}
                          onChange={(e) => setNewHttpBodyTemplate(e.target.value)}
                          placeholder='{"param": "{{value}}"}'
                          rows={3}
                        />
                      </div>
                      <div className="space-y-2">
                        <label className="text-sm font-medium">SSL Configuration</label>
                        <Select value={newHttpSslConfigId} onValueChange={setNewHttpSslConfigId}>
                          <SelectTrigger>
                            <SelectValue placeholder="Select SSL config (optional)" />
                          </SelectTrigger>
                          <SelectContent>
                            {sslConfigs.map((config) => (
                              <SelectItem key={config.id} value={config.id}>
                                {config.name}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                    </TabsContent>

                    <TabsContent value="JDBC_QUERY" className="space-y-4 mt-4">
                      <div className="space-y-2">
                        <label className="text-sm font-medium">Database Credentials *</label>
                        <Select value={newJdbcDbCredentialsId} onValueChange={setNewJdbcDbCredentialsId}>
                          <SelectTrigger>
                            <SelectValue placeholder="Select database credentials" />
                          </SelectTrigger>
                          <SelectContent>
                            {dbCredentials.map((cred) => (
                              <SelectItem key={cred.id} value={cred.id}>
                                {cred.name}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                      <div className="space-y-2">
                        <label className="text-sm font-medium">Query Template *</label>
                        <Textarea
                          value={newJdbcQuery}
                          onChange={(e) => setNewJdbcQuery(e.target.value)}
                          placeholder="SELECT * FROM users WHERE id = {{userId}}"
                          rows={4}
                        />
                      </div>
                    </TabsContent>
                  </Tabs>
                </div>
                <Button
                  onClick={handleCreateTool}
                  disabled={
                    !newToolName.trim() ||
                    (newToolType === 'HTTP_ENDPOINT' && !newHttpUrl.trim()) ||
                    (newToolType === 'JDBC_QUERY' && (!newJdbcQuery.trim() || !newJdbcDbCredentialsId))
                  }
                  className="w-full"
                >
                  Add Tool
                </Button>
              </div>
            </DialogContent>
          </Dialog>
        </header>

        {selectedToolset.description && (
          <div className="border-b bg-white px-6 py-3">
            <p className="text-sm text-slate-600">{selectedToolset.description}</p>
          </div>
        )}

        <ScrollArea className="flex-1">
          <div className="p-6">
            {selectedToolset.tools.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12 text-center">
                <Wrench className="h-12 w-12 text-slate-300" />
                <h3 className="mt-4 text-lg font-medium text-slate-700">No tools in this toolset</h3>
                <p className="mt-2 text-sm text-slate-500">
                  Add your first tool to get started
                </p>
              </div>
            ) : (
              <div className="grid gap-4">
                {selectedToolset.tools.map((tool) => (
                  <Card key={tool.id} className="transition-shadow hover:shadow-md">
                    <CardHeader className="pb-2">
                      <div className="flex items-start justify-between">
                        <div className="flex items-center gap-3">
                          <div className={`flex h-10 w-10 items-center justify-center rounded-lg ${
                            tool.type === 'HTTP_ENDPOINT' ? 'bg-blue-100' : 'bg-green-100'
                          }`}>
                            {tool.type === 'HTTP_ENDPOINT' ? (
                              <Globe className="h-5 w-5 text-blue-600" />
                            ) : (
                              <Database className="h-5 w-5 text-green-600" />
                            )}
                          </div>
                          <div>
                            <CardTitle className="text-base">{tool.name}</CardTitle>
                            <p className="text-sm text-slate-500">
                              {tool.type === 'HTTP_ENDPOINT' ? 'HTTP Endpoint' : 'JDBC Query'}
                            </p>
                          </div>
                        </div>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="text-slate-400 hover:text-red-500"
                          onClick={() => handleDeleteTool(tool.id)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </CardHeader>
                    <CardContent className="pt-0">
                      {tool.description && (
                        <p className="text-sm text-slate-600 mb-2">{tool.description}</p>
                      )}
                      {tool.type === 'HTTP_ENDPOINT' && tool.httpConfig && (
                        <div className="space-y-1 text-sm">
                          <p className="text-slate-600">
                            <Badge variant="outline" className="mr-2">{tool.httpConfig.method}</Badge>
                            {tool.httpConfig.url}
                          </p>
                        </div>
                      )}
                      {tool.type === 'JDBC_QUERY' && tool.jdbcConfig && (
                        <div className="space-y-1 text-sm">
                          <p className="text-slate-600 font-mono bg-slate-100 p-2 rounded text-xs">
                            {tool.jdbcConfig.queryTemplate}
                          </p>
                        </div>
                      )}
                    </CardContent>
                  </Card>
                ))}
              </div>
            )}
          </div>
        </ScrollArea>
      </div>
    );
  }

  return (
    <div className="flex h-screen flex-col bg-slate-50">
      <header className="flex h-14 items-center justify-between border-b bg-white px-6">
        <div className="flex items-center gap-2">
          <Wrench className="h-6 w-6 text-purple-600" />
          <h1 className="text-lg font-semibold">Toolsets</h1>
        </div>
        <Dialog open={toolsetDialogOpen} onOpenChange={setToolsetDialogOpen}>
          <DialogTrigger asChild>
            <Button className="gap-2">
              <Plus className="h-4 w-4" />
              New Toolset
            </Button>
          </DialogTrigger>
          <DialogContent className="sm:max-w-lg">
            <DialogHeader>
              <DialogTitle>Create Toolset</DialogTitle>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <label className="text-sm font-medium">Name *</label>
                <Input
                  value={newToolsetName}
                  onChange={(e) => setNewToolsetName(e.target.value)}
                  placeholder="Enter toolset name"
                />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Description</label>
                <Textarea
                  value={newToolsetDescription}
                  onChange={(e) => setNewToolsetDescription(e.target.value)}
                  placeholder="Enter toolset description"
                  rows={3}
                />
              </div>
              <Button
                onClick={handleCreateToolset}
                disabled={!newToolsetName.trim()}
                className="w-full"
              >
                Create Toolset
              </Button>
            </div>
          </DialogContent>
        </Dialog>
      </header>

      <ScrollArea className="flex-1">
        <div className="p-6">
          {isLoading ? (
            <div className="flex items-center justify-center py-12">
              <div className="text-slate-500">Loading toolsets...</div>
            </div>
          ) : toolsets.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-12 text-center">
              <Wrench className="h-12 w-12 text-slate-300" />
              <h3 className="mt-4 text-lg font-medium text-slate-700">No toolsets</h3>
              <p className="mt-2 text-sm text-slate-500">
                Create your first toolset to get started
              </p>
            </div>
          ) : (
            <div className="grid gap-4">
              {toolsets.map((toolset) => (
                <Card
                  key={toolset.id}
                  className="cursor-pointer transition-shadow hover:shadow-md"
                  onClick={() => loadToolsetDetails(toolset.id)}
                >
                  <CardHeader className="pb-2">
                    <div className="flex items-start justify-between">
                      <div className="flex items-center gap-3">
                        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-purple-100">
                          <Wrench className="h-5 w-5 text-purple-600" />
                        </div>
                        <div>
                          <CardTitle className="text-base">{toolset.name}</CardTitle>
                          <p className="text-sm text-slate-500">
                            {toolset.toolCount} tools · Created {formatDate(toolset.createdAt)}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <Button
                          variant="ghost"
                          size="icon"
                          className="text-slate-400 hover:text-red-500"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleDeleteToolset(toolset.id);
                          }}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                        <ChevronRight className="h-5 w-5 text-slate-400" />
                      </div>
                    </div>
                  </CardHeader>
                  {toolset.description && (
                    <CardContent className="pt-0">
                      <p className="text-sm text-slate-600">{toolset.description}</p>
                    </CardContent>
                  )}
                </Card>
              ))}
            </div>
          )}
        </div>
      </ScrollArea>
    </div>
  );
}
