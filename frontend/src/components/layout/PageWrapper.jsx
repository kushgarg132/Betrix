import React from 'react';
import { cn } from '@/lib/utils';

export default function PageWrapper({ children, className, fullWidth = false }) {
  return (
    <main className={cn(
      'flex-1',
      !fullWidth && 'max-w-7xl mx-auto w-full px-4 sm:px-6 py-6',
      className
    )}>
      {children}
    </main>
  );
}
