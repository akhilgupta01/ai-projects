import { useState, useEffect, useRef } from 'react';
import { FileText, Upload, Trash2, Search, X, Tag, Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { ScrollArea } from '@/components/ui/scroll-area';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import * as api from '@/services/api';
import type { DocumentResponse } from '@/types/chat';

export function DocumentsPage() {
  const [documents, setDocuments] = useState<DocumentResponse[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchTags, setSearchTags] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
  const [newDocName, setNewDocName] = useState('');
  const [newDocContent, setNewDocContent] = useState('');
  const [newDocTags, setNewDocTags] = useState('');
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    loadDocuments();
  }, []);

  const loadDocuments = async () => {
    setIsLoading(true);
    try {
      const data = await api.getAllDocuments();
      setDocuments(data);
    } catch (error) {
      console.error('Failed to load documents:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSearch = async () => {
    if (!searchQuery.trim()) {
      loadDocuments();
      return;
    }
    setIsLoading(true);
    try {
      const data = await api.searchDocuments({
        query: searchQuery,
        tags: searchTags.length > 0 ? searchTags : undefined,
      });
      setDocuments(data);
    } catch (error) {
      console.error('Failed to search documents:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleUpload = async () => {
    if (!newDocName.trim() || !newDocContent.trim()) return;

    setIsUploading(true);
    try {
      const tags = newDocTags
        .split(',')
        .map((t) => t.trim())
        .filter((t) => t.length > 0);

      await api.uploadDocument({
        name: newDocName,
        content: newDocContent,
        contentType: 'text/plain',
        tags,
      });

      setNewDocName('');
      setNewDocContent('');
      setNewDocTags('');
      setUploadDialogOpen(false);
      loadDocuments();
    } catch (error) {
      console.error('Failed to upload document:', error);
    } finally {
      setIsUploading(false);
    }
  };

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = async (e) => {
      const content = e.target?.result as string;
      setNewDocName(file.name);
      setNewDocContent(content);
      setUploadDialogOpen(true);
    };
    reader.readAsText(file);

    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleDelete = async (documentId: string) => {
    try {
      await api.deleteDocument(documentId);
      loadDocuments();
    } catch (error) {
      console.error('Failed to delete document:', error);
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const formatSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const addSearchTag = (tag: string) => {
    if (!searchTags.includes(tag)) {
      setSearchTags([...searchTags, tag]);
    }
  };

  const removeSearchTag = (tag: string) => {
    setSearchTags(searchTags.filter((t) => t !== tag));
  };

  return (
    <div className="flex h-screen flex-col bg-slate-50">
      <header className="flex h-14 items-center justify-between border-b bg-white px-6">
        <div className="flex items-center gap-2">
          <FileText className="h-6 w-6 text-blue-600" />
          <h1 className="text-lg font-semibold">Knowledge Documents</h1>
        </div>
        <div className="flex items-center gap-2">
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileUpload}
            accept=".txt,.md,.json,.csv"
            className="hidden"
          />
          <Button
            variant="outline"
            onClick={() => fileInputRef.current?.click()}
            className="gap-2"
          >
            <Upload className="h-4 w-4" />
            Upload File
          </Button>
          <Dialog open={uploadDialogOpen} onOpenChange={setUploadDialogOpen}>
            <DialogTrigger asChild>
              <Button className="gap-2">
                <Plus className="h-4 w-4" />
                New Document
              </Button>
            </DialogTrigger>
            <DialogContent className="sm:max-w-lg">
              <DialogHeader>
                <DialogTitle>Upload Knowledge Document</DialogTitle>
              </DialogHeader>
              <div className="space-y-4 py-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium">Document Name</label>
                  <Input
                    value={newDocName}
                    onChange={(e) => setNewDocName(e.target.value)}
                    placeholder="Enter document name"
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Content</label>
                  <Textarea
                    value={newDocContent}
                    onChange={(e) => setNewDocContent(e.target.value)}
                    placeholder="Enter document content or paste text here"
                    rows={8}
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Tags (comma-separated)</label>
                  <Input
                    value={newDocTags}
                    onChange={(e) => setNewDocTags(e.target.value)}
                    placeholder="e.g., operations, procedures, troubleshooting"
                  />
                </div>
                <Button
                  onClick={handleUpload}
                  disabled={!newDocName.trim() || !newDocContent.trim() || isUploading}
                  className="w-full"
                >
                  {isUploading ? 'Uploading...' : 'Upload Document'}
                </Button>
              </div>
            </DialogContent>
          </Dialog>
        </div>
      </header>

      <div className="border-b bg-white p-4">
        <div className="mx-auto max-w-4xl space-y-3">
          <div className="flex gap-2">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <Input
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                placeholder="Search documents by name, content, or tags..."
                className="pl-10"
              />
            </div>
            <Button onClick={handleSearch}>Search</Button>
            {(searchQuery || searchTags.length > 0) && (
              <Button
                variant="outline"
                onClick={() => {
                  setSearchQuery('');
                  setSearchTags([]);
                  loadDocuments();
                }}
              >
                Clear
              </Button>
            )}
          </div>
          {searchTags.length > 0 && (
            <div className="flex flex-wrap gap-2">
              <span className="text-sm text-slate-500">Filter by tags:</span>
              {searchTags.map((tag) => (
                <Badge key={tag} variant="secondary" className="gap-1">
                  {tag}
                  <button onClick={() => removeSearchTag(tag)}>
                    <X className="h-3 w-3" />
                  </button>
                </Badge>
              ))}
            </div>
          )}
        </div>
      </div>

      <ScrollArea className="flex-1">
        <div className="mx-auto max-w-4xl p-6">
          {isLoading ? (
            <div className="flex items-center justify-center py-12">
              <div className="text-slate-500">Loading documents...</div>
            </div>
          ) : documents.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-12 text-center">
              <FileText className="h-12 w-12 text-slate-300" />
              <h3 className="mt-4 text-lg font-medium text-slate-700">No documents found</h3>
              <p className="mt-2 text-sm text-slate-500">
                {searchQuery
                  ? 'Try adjusting your search query'
                  : 'Upload your first knowledge document to get started'}
              </p>
            </div>
          ) : (
            <div className="grid gap-4">
              {documents.map((doc) => (
                <Card key={doc.id} className="transition-shadow hover:shadow-md">
                  <CardHeader className="pb-2">
                    <div className="flex items-start justify-between">
                      <div className="flex items-center gap-3">
                        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-100">
                          <FileText className="h-5 w-5 text-blue-600" />
                        </div>
                        <div>
                          <CardTitle className="text-base">{doc.name}</CardTitle>
                          <p className="text-sm text-slate-500">
                            {formatSize(doc.size)} · {formatDate(doc.uploadedAt)}
                          </p>
                        </div>
                      </div>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="text-slate-400 hover:text-red-500"
                        onClick={() => handleDelete(doc.id)}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </CardHeader>
                  {doc.tags.length > 0 && (
                    <CardContent className="pt-0">
                      <div className="flex flex-wrap gap-2">
                        {doc.tags.map((tag) => (
                          <Badge
                            key={tag}
                            variant="outline"
                            className="cursor-pointer gap-1 hover:bg-slate-100"
                            onClick={() => addSearchTag(tag)}
                          >
                            <Tag className="h-3 w-3" />
                            {tag}
                          </Badge>
                        ))}
                      </div>
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
