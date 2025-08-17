# 🚗 UberClone Frontend

A modern, responsive React frontend for the UberClone ride-sharing platform. Built with TypeScript, Tailwind CSS, and cutting-edge web technologies.

## ✨ Features

### 🎨 **Modern UI/UX**
- **Responsive Design**: Mobile-first approach with desktop optimization
- **Smooth Animations**: Framer Motion for delightful user interactions
- **Custom Components**: Reusable UI components with Tailwind CSS
- **Dark/Light Mode**: Theme support (coming soon)

### 🗺️ **Location Services**
- **Real-time GPS**: Current location detection and tracking
- **Geocoding**: Address lookup and reverse geocoding
- **Map Integration**: Google Maps integration for ride booking
- **Location History**: Save and manage favorite locations

### 🔐 **Authentication & Security**
- **JWT Authentication**: Secure token-based authentication
- **Protected Routes**: Role-based access control
- **Form Validation**: Client-side validation with error handling
- **Secure Storage**: Local storage with encryption

### 📱 **Real-time Features**
- **Live Tracking**: Real-time driver and ride tracking
- **WebSocket Support**: Instant updates and notifications
- **Push Notifications**: Browser notifications for ride updates
- **Offline Support**: PWA capabilities for offline usage

### 🚀 **Performance & Optimization**
- **Code Splitting**: Lazy loading for better performance
- **Image Optimization**: WebP support and lazy loading
- **Bundle Optimization**: Tree shaking and minification
- **Caching**: Service worker for offline functionality

## 🛠️ Tech Stack

### **Core Technologies**
- **React 18**: Latest React with concurrent features
- **TypeScript**: Type-safe development
- **Vite**: Fast build tool and dev server
- **Tailwind CSS**: Utility-first CSS framework

### **State Management**
- **Zustand**: Lightweight state management
- **React Query**: Server state management
- **Context API**: Local state management

### **UI & Animation**
- **Framer Motion**: Production-ready motion library
- **Lucide React**: Beautiful, customizable icons
- **Headless UI**: Unstyled, accessible UI components
- **React Hook Form**: Performant forms with validation

### **Development Tools**
- **ESLint**: Code quality and consistency
- **Prettier**: Code formatting
- **Husky**: Git hooks for code quality
- **TypeScript**: Static type checking

## 🚀 Getting Started

### **Prerequisites**
- Node.js 18+ 
- npm or yarn
- Git

### **Installation**

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd uber-clone/frontend
   ```

2. **Install dependencies**
   ```bash
   npm install
   # or
   yarn install
   ```

3. **Environment Setup**
   ```bash
   cp .env.example .env.local
   ```
   
   Configure your environment variables:
   ```env
   REACT_APP_API_URL=http://localhost:8080
   REACT_APP_GO_SERVICE_URL=http://localhost:8081
   REACT_APP_ML_SERVICE_URL=http://localhost:8000
   REACT_APP_GOOGLE_MAPS_API_KEY=your_google_maps_api_key
   ```

4. **Start development server**
   ```bash
   npm run dev
   # or
   yarn dev
   ```

5. **Open your browser**
   Navigate to `http://localhost:3000`

## 📁 Project Structure

```
src/
├── components/          # Reusable UI components
│   ├── Auth/           # Authentication components
│   ├── Layout/         # Layout and navigation
│   ├── Ride/           # Ride-related components
│   └── ui/             # Base UI components
├── contexts/            # React contexts
│   ├── AuthContext.tsx # Authentication state
│   ├── RideContext.tsx # Ride management state
│   └── LocationContext.tsx # Location services
├── hooks/               # Custom React hooks
├── pages/               # Page components
│   ├── Auth/           # Authentication pages
│   ├── Dashboard/       # Dashboard pages
│   ├── Ride/            # Ride management pages
│   └── Profile/         # User profile pages
├── services/            # API services
├── types/               # TypeScript type definitions
├── utils/               # Utility functions
└── styles/              # Global styles and CSS
```

## 🎯 Available Scripts

### **Development**
```bash
npm run dev          # Start development server
npm run build        # Build for production
npm run preview      # Preview production build
```

### **Code Quality**
```bash
npm run lint         # Run ESLint
npm run lint:fix     # Fix ESLint errors
npm run type-check   # Run TypeScript compiler
npm run format       # Format code with Prettier
```

### **Testing**
```bash
npm run test         # Run tests
npm run test:watch   # Run tests in watch mode
npm run test:coverage # Run tests with coverage
```

## 🔧 Configuration

### **Tailwind CSS**
The project uses Tailwind CSS with custom configuration:
- Custom color palette
- Responsive breakpoints
- Custom animations
- Component classes

### **TypeScript**
Strict TypeScript configuration with:
- Strict mode enabled
- No implicit any
- Strict null checks
- Module resolution

### **ESLint & Prettier**
Code quality rules:
- React best practices
- TypeScript rules
- Import sorting
- Consistent formatting

## 📱 Responsive Design

### **Breakpoints**
- **Mobile**: 320px - 768px
- **Tablet**: 768px - 1024px
- **Desktop**: 1024px+

### **Mobile-First Approach**
- Touch-friendly interactions
- Optimized for mobile devices
- Progressive enhancement

## 🚀 Deployment

### **Build for Production**
```bash
npm run build
```

### **Deploy to Vercel**
```bash
npm install -g vercel
vercel --prod
```

### **Deploy to Netlify**
```bash
npm run build
# Upload dist/ folder to Netlify
```

## 🔒 Security Features

- **HTTPS Only**: Secure connections
- **CSP Headers**: Content Security Policy
- **XSS Protection**: Cross-site scripting prevention
- **CSRF Protection**: Cross-site request forgery prevention

## 📊 Performance Monitoring

- **Core Web Vitals**: LCP, FID, CLS tracking
- **Bundle Analysis**: Webpack bundle analyzer
- **Performance Budgets**: Size and performance limits
- **Lighthouse**: Automated performance testing

## 🤝 Contributing

### **Code Style**
- Follow TypeScript best practices
- Use functional components with hooks
- Implement proper error boundaries
- Write meaningful commit messages

### **Pull Request Process**
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 🐛 Troubleshooting

### **Common Issues**

1. **Port already in use**
   ```bash
   # Kill process on port 3000
   lsof -ti:3000 | xargs kill -9
   ```

2. **TypeScript errors**
   ```bash
   npm run type-check
   # Fix any type issues
   ```

3. **Build failures**
   ```bash
   rm -rf node_modules package-lock.json
   npm install
   npm run build
   ```

### **Performance Issues**
- Check bundle size with `npm run analyze`
- Optimize images and assets
- Implement code splitting
- Use React.memo for expensive components

## 📚 Additional Resources

- [React Documentation](https://reactjs.org/)
- [TypeScript Handbook](https://www.typescriptlang.org/docs/)
- [Tailwind CSS Docs](https://tailwindcss.com/docs)
- [Framer Motion](https://www.framer.com/motion/)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- React team for the amazing framework
- Tailwind CSS for the utility-first approach
- Framer Motion for smooth animations
- All contributors and maintainers

---

**Happy coding! 🚀**

For support, please open an issue or contact the development team. 