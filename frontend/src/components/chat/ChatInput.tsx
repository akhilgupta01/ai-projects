import { useState, useRef, useEffect } from "react";
import { Send, Paperclip, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";

export interface DocumentAttachment {
  name: string;
  content: string;
  contentType: string;
  size: number;
}

interface ChatInputProps {
  onSendMessage: (message: string, attachments?: DocumentAttachment[]) => void;
  disabled?: boolean;
}

export function ChatInput({ onSendMessage, disabled }: ChatInputProps) {
  const [message, setMessage] = useState("");
  const [attachments, setAttachments] = useState<DocumentAttachment[]>([]);
  const [isDragging, setIsDragging] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = "auto";
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 200)}px`;
    }
  }, [message]);

  const handleFileSelect = async (files: FileList) => {
    const newAttachments: DocumentAttachment[] = [];

    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      try {
        const content = await readFileAsText(file);
        newAttachments.push({
          name: file.name,
          content,
          contentType: file.type || "text/plain",
          size: file.size,
        });
      } catch (error) {
        console.error(`Failed to read file ${file.name}:`, error);
      }
    }

    setAttachments((prev) => [...prev, ...newAttachments]);
  };

  const readFileAsText = (file: File): Promise<string> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const result = e.target?.result;
        if (typeof result === "string") {
          resolve(result);
        } else {
          reject(new Error("Failed to read file"));
        }
      };
      reader.onerror = () => reject(new Error("Failed to read file"));
      reader.readAsText(file);
    });
  };

  const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);

    if (e.dataTransfer.files) {
      handleFileSelect(e.dataTransfer.files);
    }
  };

  const handleSubmit = () => {
    if ((message.trim() || attachments.length > 0) && !disabled) {
      onSendMessage(
        message.trim() || `[${attachments.length} document(s) attached]`,
        attachments,
      );
      setMessage("");
      setAttachments([]);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  const removeAttachment = (index: number) => {
    setAttachments((prev) => prev.filter((_, i) => i !== index));
  };

  const formatSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div className="border-t bg-white p-4">
      <div className="mx-auto max-w-3xl space-y-3">
        {/* Attachments Display */}
        {attachments.length > 0 && (
          <div className="flex flex-wrap gap-2">
            {attachments.map((attachment, index) => (
              <Badge key={index} variant="secondary" className="gap-2 py-1.5">
                <span className="text-xs">{attachment.name}</span>
                <span className="text-xs text-slate-500">
                  ({formatSize(attachment.size)})
                </span>
                <button
                  onClick={() => removeAttachment(index)}
                  className="ml-1 hover:text-slate-700"
                >
                  <X className="h-3 w-3" />
                </button>
              </Badge>
            ))}
          </div>
        )}

        {/* Input Area */}
        <div
          className={`flex gap-4 rounded-lg border-2 transition-colors ${
            isDragging
              ? "border-blue-400 bg-blue-50"
              : "border-slate-200 bg-white"
          } p-3`}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
        >
          <Textarea
            ref={textareaRef}
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Type your message here... (Press Enter to send, Shift+Enter for new line)"
            className="min-h-[44px] max-h-[200px] resize-none border-0 bg-transparent p-0 focus:ring-0"
            disabled={disabled}
            rows={1}
          />
          <div className="flex shrink-0 flex-col gap-2">
            <Button
              onClick={() => fileInputRef.current?.click()}
              disabled={disabled}
              size="icon"
              variant="ghost"
              className="h-10 w-10 hover:bg-slate-100"
              title="Attach documents (drag & drop or click)"
            >
              <Paperclip className="h-4 w-4" />
            </Button>
            <Button
              onClick={handleSubmit}
              disabled={
                (!message.trim() && attachments.length === 0) || disabled
              }
              size="icon"
              className="h-10 w-10 shrink-0"
            >
              <Send className="h-4 w-4" />
            </Button>
          </div>
        </div>

        {/* Hidden File Input */}
        <input
          ref={fileInputRef}
          type="file"
          multiple
          onChange={(e) => e.target.files && handleFileSelect(e.target.files)}
          accept=".txt,.md,.json,.csv,.pdf,.doc,.docx"
          className="hidden"
        />

        {/* Drag & Drop Hint */}
        {isDragging && (
          <p className="text-center text-sm text-blue-600">
            Drop files here to attach them to your message
          </p>
        )}
      </div>
    </div>
  );
}
