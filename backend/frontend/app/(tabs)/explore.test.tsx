import React from 'react';
import { render } from '@testing-library/react-native';
import TabTwoScreen from './explore';

describe('TabTwoScreen', () => {
  it('renders the Explore title', () => {
    const { getByText } = render(<TabTwoScreen />);
    expect(getByText('Explore')).toBeTruthy();
  });
}); 