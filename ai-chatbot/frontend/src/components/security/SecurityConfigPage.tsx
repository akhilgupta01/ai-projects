import { useState, useEffect } from 'react';
import { Shield, Database, Lock, Trash2, Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Switch } from '@/components/ui/switch';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import * as api from '@/services/api';
import type { SslConfig, DatabaseCredentials } from '@/types/chat';

export function SecurityConfigPage() {
  const [sslConfigs, setSslConfigs] = useState<SslConfig[]>([]);
  const [dbCredentials, setDbCredentials] = useState<DatabaseCredentials[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('ssl');

  const [sslDialogOpen, setSslDialogOpen] = useState(false);
  const [dbDialogOpen, setDbDialogOpen] = useState(false);

  const [newSslName, setNewSslName] = useState('');
  const [newSslDescription, setNewSslDescription] = useState('');
  const [newSslTrustStorePath, setNewSslTrustStorePath] = useState('');
  const [newSslTrustStorePassword, setNewSslTrustStorePassword] = useState('');
  const [newSslKeyStorePath, setNewSslKeyStorePath] = useState('');
  const [newSslKeyStorePassword, setNewSslKeyStorePassword] = useState('');
  const [newSslVerifyHostname, setNewSslVerifyHostname] = useState(true);

  const [newDbName, setNewDbName] = useState('');
  const [newDbDescription, setNewDbDescription] = useState('');
  const [newDbJdbcUrl, setNewDbJdbcUrl] = useState('');
  const [newDbUsername, setNewDbUsername] = useState('');
  const [newDbPassword, setNewDbPassword] = useState('');
  const [newDbDriverClassName, setNewDbDriverClassName] = useState('');

  useEffect(() => {
    loadConfigs();
  }, []);

  const loadConfigs = async () => {
    setIsLoading(true);
    try {
      const [ssl, db] = await Promise.all([
        api.getAllSslConfigs(),
        api.getAllDatabaseCredentials(),
      ]);
      setSslConfigs(ssl);
      setDbCredentials(db);
    } catch (error) {
      console.error('Failed to load security configs:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateSslConfig = async () => {
    if (!newSslName.trim()) return;

    try {
      await api.createSslConfig({
        name: newSslName,
        description: newSslDescription || undefined,
        trustStorePath: newSslTrustStorePath || undefined,
        trustStorePassword: newSslTrustStorePassword || undefined,
        keyStorePath: newSslKeyStorePath || undefined,
        keyStorePassword: newSslKeyStorePassword || undefined,
        verifyHostname: newSslVerifyHostname,
      });

      resetSslForm();
      setSslDialogOpen(false);
      loadConfigs();
    } catch (error) {
      console.error('Failed to create SSL config:', error);
    }
  };

  const handleCreateDbCredentials = async () => {
    if (!newDbName.trim() || !newDbJdbcUrl.trim() || !newDbUsername.trim()) return;

    try {
      await api.createDatabaseCredentials({
        name: newDbName,
        description: newDbDescription || undefined,
        jdbcUrl: newDbJdbcUrl,
        username: newDbUsername,
        password: newDbPassword,
        driverClassName: newDbDriverClassName || undefined,
      });

      resetDbForm();
      setDbDialogOpen(false);
      loadConfigs();
    } catch (error) {
      console.error('Failed to create database credentials:', error);
    }
  };

  const handleDeleteSslConfig = async (id: string) => {
    try {
      await api.deleteSslConfig(id);
      loadConfigs();
    } catch (error) {
      console.error('Failed to delete SSL config:', error);
    }
  };

  const handleDeleteDbCredentials = async (id: string) => {
    try {
      await api.deleteDatabaseCredentials(id);
      loadConfigs();
    } catch (error) {
      console.error('Failed to delete database credentials:', error);
    }
  };

  const resetSslForm = () => {
    setNewSslName('');
    setNewSslDescription('');
    setNewSslTrustStorePath('');
    setNewSslTrustStorePassword('');
    setNewSslKeyStorePath('');
    setNewSslKeyStorePassword('');
    setNewSslVerifyHostname(true);
  };

  const resetDbForm = () => {
    setNewDbName('');
    setNewDbDescription('');
    setNewDbJdbcUrl('');
    setNewDbUsername('');
    setNewDbPassword('');
    setNewDbDriverClassName('');
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  };

  return (
    <div className="flex h-screen flex-col bg-slate-50">
      <header className="flex h-14 items-center justify-between border-b bg-white px-6">
        <div className="flex items-center gap-2">
          <Shield className="h-6 w-6 text-green-600" />
          <h1 className="text-lg font-semibold">Security Configurations</h1>
        </div>
      </header>

      <div className="flex-1 overflow-hidden p-6">
        <Tabs value={activeTab} onValueChange={setActiveTab} className="h-full flex flex-col">
          <div className="flex items-center justify-between mb-4">
            <TabsList>
              <TabsTrigger value="ssl" className="gap-2">
                <Lock className="h-4 w-4" />
                SSL Configurations
              </TabsTrigger>
              <TabsTrigger value="database" className="gap-2">
                <Database className="h-4 w-4" />
                Database Credentials
              </TabsTrigger>
            </TabsList>

            {activeTab === 'ssl' ? (
              <Dialog open={sslDialogOpen} onOpenChange={setSslDialogOpen}>
                <DialogTrigger asChild>
                  <Button className="gap-2">
                    <Plus className="h-4 w-4" />
                    New SSL Config
                  </Button>
                </DialogTrigger>
                <DialogContent className="sm:max-w-lg">
                  <DialogHeader>
                    <DialogTitle>Create SSL Configuration</DialogTitle>
                  </DialogHeader>
                  <div className="space-y-4 py-4">
                    <div className="space-y-2">
                      <label className="text-sm font-medium">Name *</label>
                      <Input
                        value={newSslName}
                        onChange={(e) => setNewSslName(e.target.value)}
                        placeholder="Enter configuration name"
                      />
                    </div>
                    <div className="space-y-2">
                      <label className="text-sm font-medium">Description</label>
                      <Textarea
                        value={newSslDescription}
                        onChange={(e) => setNewSslDescription(e.target.value)}
                        placeholder="Enter description"
                        rows={2}
                      />
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <label className="text-sm font-medium">Trust Store Path</label>
                        <Input
                          value={newSslTrustStorePath}
                          onChange={(e) => setNewSslTrustStorePath(e.target.value)}
                          placeholder="/path/to/truststore.jks"
                        />
                      </div>
                      <div className="space-y-2">
                        <label className="text-sm font-medium">Trust Store Password</label>
                        <Input
                          type="password"
                          value={newSslTrustStorePassword}
                          onChange={(e) => setNewSslTrustStorePassword(e.target.value)}
                          placeholder="Password"
                        />
                      </div>
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <label className="text-sm font-medium">Key Store Path</label>
                        <Input
                          value={newSslKeyStorePath}
                          onChange={(e) => setNewSslKeyStorePath(e.target.value)}
                          placeholder="/path/to/keystore.jks"
                        />
                      </div>
                      <div className="space-y-2">
                        <label className="text-sm font-medium">Key Store Password</label>
                        <Input
                          type="password"
                          value={newSslKeyStorePassword}
                          onChange={(e) => setNewSslKeyStorePassword(e.target.value)}
                          placeholder="Password"
                        />
                      </div>
                    </div>
                    <div className="flex items-center justify-between">
                      <label className="text-sm font-medium">Verify Hostname</label>
                      <Switch
                        checked={newSslVerifyHostname}
                        onCheckedChange={setNewSslVerifyHostname}
                      />
                    </div>
                    <Button
                      onClick={handleCreateSslConfig}
                      disabled={!newSslName.trim()}
                      className="w-full"
                    >
                      Create SSL Configuration
                    </Button>
                  </div>
                </DialogContent>
              </Dialog>
            ) : (
              <Dialog open={dbDialogOpen} onOpenChange={setDbDialogOpen}>
                <DialogTrigger asChild>
                  <Button className="gap-2">
                    <Plus className="h-4 w-4" />
                    New Database Credentials
                  </Button>
                </DialogTrigger>
                <DialogContent className="sm:max-w-lg">
                  <DialogHeader>
                    <DialogTitle>Create Database Credentials</DialogTitle>
                  </DialogHeader>
                  <div className="space-y-4 py-4">
                    <div className="space-y-2">
                      <label className="text-sm font-medium">Name *</label>
                      <Input
                        value={newDbName}
                        onChange={(e) => setNewDbName(e.target.value)}
                        placeholder="Enter credentials name"
                      />
                    </div>
                    <div className="space-y-2">
                      <label className="text-sm font-medium">Description</label>
                      <Textarea
                        value={newDbDescription}
                        onChange={(e) => setNewDbDescription(e.target.value)}
                        placeholder="Enter description"
                        rows={2}
                      />
                    </div>
                    <div className="space-y-2">
                      <label className="text-sm font-medium">JDBC URL *</label>
                      <Input
                        value={newDbJdbcUrl}
                        onChange={(e) => setNewDbJdbcUrl(e.target.value)}
                        placeholder="jdbc:postgresql://localhost:5432/mydb"
                      />
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <label className="text-sm font-medium">Username *</label>
                        <Input
                          value={newDbUsername}
                          onChange={(e) => setNewDbUsername(e.target.value)}
                          placeholder="Username"
                        />
                      </div>
                      <div className="space-y-2">
                        <label className="text-sm font-medium">Password *</label>
                        <Input
                          type="password"
                          value={newDbPassword}
                          onChange={(e) => setNewDbPassword(e.target.value)}
                          placeholder="Password"
                        />
                      </div>
                    </div>
                    <div className="space-y-2">
                      <label className="text-sm font-medium">Driver Class Name</label>
                      <Input
                        value={newDbDriverClassName}
                        onChange={(e) => setNewDbDriverClassName(e.target.value)}
                        placeholder="org.postgresql.Driver"
                      />
                    </div>
                    <Button
                      onClick={handleCreateDbCredentials}
                      disabled={!newDbName.trim() || !newDbJdbcUrl.trim() || !newDbUsername.trim()}
                      className="w-full"
                    >
                      Create Database Credentials
                    </Button>
                  </div>
                </DialogContent>
              </Dialog>
            )}
          </div>

          <TabsContent value="ssl" className="flex-1 mt-0">
            <ScrollArea className="h-full">
              {isLoading ? (
                <div className="flex items-center justify-center py-12">
                  <div className="text-slate-500">Loading configurations...</div>
                </div>
              ) : sslConfigs.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-12 text-center">
                  <Lock className="h-12 w-12 text-slate-300" />
                  <h3 className="mt-4 text-lg font-medium text-slate-700">No SSL configurations</h3>
                  <p className="mt-2 text-sm text-slate-500">
                    Create your first SSL configuration to get started
                  </p>
                </div>
              ) : (
                <div className="grid gap-4">
                  {sslConfigs.map((config) => (
                    <Card key={config.id} className="transition-shadow hover:shadow-md">
                      <CardHeader className="pb-2">
                        <div className="flex items-start justify-between">
                          <div className="flex items-center gap-3">
                            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-green-100">
                              <Lock className="h-5 w-5 text-green-600" />
                            </div>
                            <div>
                              <CardTitle className="text-base">{config.name}</CardTitle>
                              <p className="text-sm text-slate-500">
                                Created {formatDate(config.createdAt)}
                              </p>
                            </div>
                          </div>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="text-slate-400 hover:text-red-500"
                            onClick={() => handleDeleteSslConfig(config.id)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </CardHeader>
                      <CardContent className="pt-0">
                        {config.description && (
                          <p className="text-sm text-slate-600 mb-2">{config.description}</p>
                        )}
                        <div className="flex flex-wrap gap-2">
                          <Badge variant="outline">
                            Verify Hostname: {config.verifyHostname ? 'Yes' : 'No'}
                          </Badge>
                          {config.trustStorePath && (
                            <Badge variant="secondary">Trust Store Configured</Badge>
                          )}
                          {config.keyStorePath && (
                            <Badge variant="secondary">Key Store Configured</Badge>
                          )}
                        </div>
                      </CardContent>
                    </Card>
                  ))}
                </div>
              )}
            </ScrollArea>
          </TabsContent>

          <TabsContent value="database" className="flex-1 mt-0">
            <ScrollArea className="h-full">
              {isLoading ? (
                <div className="flex items-center justify-center py-12">
                  <div className="text-slate-500">Loading credentials...</div>
                </div>
              ) : dbCredentials.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-12 text-center">
                  <Database className="h-12 w-12 text-slate-300" />
                  <h3 className="mt-4 text-lg font-medium text-slate-700">No database credentials</h3>
                  <p className="mt-2 text-sm text-slate-500">
                    Create your first database credentials to get started
                  </p>
                </div>
              ) : (
                <div className="grid gap-4">
                  {dbCredentials.map((cred) => (
                    <Card key={cred.id} className="transition-shadow hover:shadow-md">
                      <CardHeader className="pb-2">
                        <div className="flex items-start justify-between">
                          <div className="flex items-center gap-3">
                            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-100">
                              <Database className="h-5 w-5 text-blue-600" />
                            </div>
                            <div>
                              <CardTitle className="text-base">{cred.name}</CardTitle>
                              <p className="text-sm text-slate-500">
                                Created {formatDate(cred.createdAt)}
                              </p>
                            </div>
                          </div>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="text-slate-400 hover:text-red-500"
                            onClick={() => handleDeleteDbCredentials(cred.id)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </CardHeader>
                      <CardContent className="pt-0">
                        {cred.description && (
                          <p className="text-sm text-slate-600 mb-2">{cred.description}</p>
                        )}
                        <div className="space-y-1 text-sm">
                          <p className="text-slate-600">
                            <span className="font-medium">JDBC URL:</span> {cred.jdbcUrl}
                          </p>
                          <p className="text-slate-600">
                            <span className="font-medium">Username:</span> {cred.username}
                          </p>
                          {cred.driverClassName && (
                            <p className="text-slate-600">
                              <span className="font-medium">Driver:</span> {cred.driverClassName}
                            </p>
                          )}
                        </div>
                      </CardContent>
                    </Card>
                  ))}
                </div>
              )}
            </ScrollArea>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
}
