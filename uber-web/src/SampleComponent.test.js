import React from 'react';
import { render, screen } from '@testing-library/react';
import SampleComponent from './SampleComponent';

test('renders the message prop', () => {
  render(<SampleComponent message="Hello, Uber!" />);
  expect(screen.getByText('Hello, Uber!')).toBeInTheDocument();
}); 