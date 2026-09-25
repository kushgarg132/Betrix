import * as React from 'react';
import { cva } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const badgeVariants = cva(
  'inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold transition-colors select-none',
  {
    variants: {
      variant: {
        default:  'bg-neon-cyan/15 text-neon-cyan border border-neon-cyan/40',
        surface:  'bg-surface-elevated text-text-muted border border-border',
        success:  'bg-success/15 text-success border border-success/40',
        danger:   'bg-danger/15 text-danger border border-danger/40',
        warning:  'bg-warning/15 text-warning border border-warning/40',
        info:     'bg-neon-cyan/15 text-neon-cyan border border-neon-cyan/40',
        waiting:  'bg-warning/15 text-warning border border-warning/40',
        active:   'bg-neon-cyan/15 text-neon-cyan border border-neon-cyan/40',
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
