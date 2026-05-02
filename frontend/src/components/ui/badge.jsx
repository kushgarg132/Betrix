import * as React from 'react';
import { cva } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const badgeVariants = cva(
  'inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold transition-colors select-none',
  {
    variants: {
      variant: {
        default:  'bg-gold-muted text-gold border border-border-gold',
        surface:  'bg-surface-elevated text-text-muted border border-border',
        success:  'bg-success-muted text-success-light border border-success/30',
        danger:   'bg-danger-muted text-danger-light border border-danger/30',
        warning:  'bg-amber-900/20 text-amber-400 border border-amber-700/30',
        info:     'bg-blue-900/20 text-blue-400 border border-blue-700/30',
        waiting:  'bg-amber-900/20 text-amber-400 border border-amber-700/30',
        active:   'bg-success-muted text-success-light border border-success/30',
        completed:'bg-surface-elevated text-text-dim border border-border',
      },
    },
    defaultVariants: { variant: 'default' },
  }
);

function Badge({ className, variant, ...props }) {
  return <span className={cn(badgeVariants({ variant }), className)} {...props} />;
}

export { Badge, badgeVariants };
