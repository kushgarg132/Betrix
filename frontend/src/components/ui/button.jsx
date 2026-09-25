import * as React from 'react';
import { cva } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-[var(--radius)] text-sm font-medium transition-all duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-neon-cyan focus-visible:ring-offset-2 focus-visible:ring-offset-background disabled:pointer-events-none disabled:opacity-40 active:scale-[0.97] select-none cursor-pointer',
  {
    variants: {
      variant: {
        default: 'bg-neon-cyan text-text-inverse font-semibold hover:shadow-[var(--glow-md)]',
        outline: 'border border-neon-cyan text-neon-cyan bg-transparent hover:bg-neon-cyan/10',
        ghost:   'text-text-muted hover:bg-surface-elevated hover:text-text',
        danger:  'bg-danger/15 text-danger border border-danger/40',
        success: 'bg-success/15 text-success border border-success/40',
        surface: 'bg-surface-elevated text-text border border-border-strong',
        link:    'text-neon-cyan underline-offset-4 hover:underline p-0 h-auto',
      },
      size: {
        default: 'h-11 px-4 py-2',
        sm:  'h-9 px-3 text-xs rounded-[var(--radius-sm)]',
        lg:  'h-12 px-6 text-base',
        xl:  'h-14 px-8 text-base font-semibold',
        icon: 'size-11 p-0',
        'icon-sm': 'size-9 p-0',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  }
);

const Button = React.forwardRef(({ className, variant, size, ...props }, ref) => (
  <button
    ref={ref}
    className={cn(buttonVariants({ variant, size }), className)}
    {...props}
  />
));

Button.displayName = 'Button';

export { Button, buttonVariants };
