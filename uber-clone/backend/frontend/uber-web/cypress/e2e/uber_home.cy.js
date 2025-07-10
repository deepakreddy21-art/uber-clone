describe('Uber Web App', () => {
  it('should load the home page and display React logo', () => {
    cy.visit('http://localhost:3000');
    cy.get('img[alt="logo"]').should('exist');
  });
}); 