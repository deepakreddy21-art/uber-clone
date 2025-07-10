import React from 'react';
import { render } from '@testing-library/react-native';
import LoginScreen from '../LoginScreen';

test('renders login screen', () => {
  const { getByText, getByPlaceholderText } = render(<LoginScreen navigation={{ navigate: jest.fn(), replace: jest.fn() }} />);
  expect(getByText('Login')).toBeTruthy();
  expect(getByPlaceholderText('Email')).toBeTruthy();
  expect(getByPlaceholderText('Password')).toBeTruthy();
}); 