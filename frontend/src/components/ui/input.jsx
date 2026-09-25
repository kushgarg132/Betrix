import * as React from 'react';
import { cn } from '@/lib/utils';

const Input = React.forwardRef(({ className, type, ...props }, ref) => (
  <input
    ref={ref}
    type={type}
    className={cn(
      'flex h-11 w-full rounded-[var(--radius)] border border-border-strong bg-surface px-3 py-2',
      'text-sm text-text placeholder:text-text-dim',
      'transition-colors duration-150',
      'hover:border-border-strong',
      'focus-visible:outline-none focus-visible:border-neon-cyan focus-visible:ring-0',
      'disabled:cursor-not-allowed disabled:opacity-50',
      'file:border-0 file:bg-transparent file:text-sm file:font-medium',
      className
    )}
    {...props}
  />
));

Input.displayName = 'Input';

export { Input };
