import * as React from 'react';
import { cva } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-[var(--radius)] text-sm font-medium transition-all duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-gold focus-visible:ring-offset-2 focus-visible:ring-offset-background disabled:pointer-events-none disabled:opacity-40 active:scale-[0.97] select-none cursor-pointer',
  {
    variants: {
      variant: {
        default: 'bg-gold text-text-inverse font-semibold hover:bg-gold-light shadow-md hover:shadow-[0_0_20px_rgba(212,168,67,0.4)]',
        outline: 'border border-border-gold text-gold hover:bg-gold-muted hover:border-gold',
        ghost:   'text-text-muted hover:bg-surface-elevated hover:text-text',
        danger:  'bg-danger text-white hover:bg-red-500 shadow-md',
        success: 'bg-success text-white hover:bg-green-400 shadow-md',
        surface: 'bg-surface-elevated text-text hover:bg-surface-overlay border border-border',
        link:    'text-gold underline-offset-4 hover:underline p-0 h-auto',
      },
      size: {
        default: 'h-10 px-4 py-2',
        sm:  'h-8 px-3 text-xs rounded-[var(--radius-sm)]',
        lg:  'h-12 px-6 text-base',
        xl:  'h-14 px-8 text-base font-semibold',
        icon: 'h-10 w-10 p-0',
        'icon-sm': 'h-8 w-8 p-0',
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
