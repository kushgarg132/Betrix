import React from 'react';
import { Button } from '@/components/ui/button';
import { PlusCircle } from 'lucide-react';

export default function EmptyLobbyState({ onCreate }) {
  return (
    <div className="flex flex-col items-center justify-center py-24 text-center">
      {/* Felt mini-table illustration */}
      <div
        className="w-28 h-16 rounded-full mb-6 felt-surface opacity-60"
        style={{ boxShadow: '0 4px 24px rgba(0,0,0,0.5)' }}
      />
      <h3 className="text-text font-semibold text-lg mb-2">No tables open</h3>
      <p className="text-text-muted text-sm max-w-xs mb-6">
        Be the first to deal. Create a table and invite others to play.
      </p>
      <Button onClick={onCreate} className="gap-2">
        <PlusCircle size={16} />
        Create Table
      </Button>
    </div>
  );
}
