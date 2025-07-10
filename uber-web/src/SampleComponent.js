import React from 'react';
import PropTypes from 'prop-types';

const SampleComponent = ({ message }) => (
  <div>{message}</div>
);

SampleComponent.propTypes = {
  message: PropTypes.string.isRequired,
};

export default SampleComponent; 