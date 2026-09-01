import { Component, type ErrorInfo, type ReactNode } from 'react';

import { ErrorState } from './ErrorState.tsx';

interface RouteErrorBoundaryProps {
  children: ReactNode;
}

interface RouteErrorBoundaryState {
  hasError: boolean;
}

export class RouteErrorBoundary extends Component<RouteErrorBoundaryProps, RouteErrorBoundaryState> {
  state: RouteErrorBoundaryState = { hasError: false };

  static getDerivedStateFromError(): RouteErrorBoundaryState {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Route render failed', error, info);
  }

  render() {
    if (this.state.hasError) {
      return (
        <ErrorState
          title="Unable to display this page"
          description="An unexpected application error occurred while rendering this page. Please try again."
        />
      );
    }

    return this.props.children;
  }
}
