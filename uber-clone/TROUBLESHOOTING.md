# 🚗 Uber Clone - Troubleshooting Guide

## 🚨 **Common Issues & Solutions**

### **1. Docker Not Found**
**Error**: `'docker' is not recognized as an internal or external command`

**Solution**:
1. Install Docker Desktop for Windows: https://www.docker.com/products/docker-desktop/
2. Restart your computer after installation
3. Verify installation: `docker --version`

### **2. Port Already in Use**
**Error**: `Bind for 0.0.0.0:8080 failed: port is already allocated`

**Solution**:
```bash
# Check what's using the port
netstat -ano | findstr :8080

# Kill the process (replace PID with actual process ID)
taskkill /PID <PID> /F

# Or change the port in docker-compose.yml
```

### **3. Database Connection Failed**
**Error**: `Connection to postgres:5432 refused`

**Solution**:
1. Check if PostgreSQL container is running: `docker compose ps`
2. Check container logs: `docker compose logs postgres`
3. Restart services: `docker compose restart`

### **4. Backend Won't Start**
**Error**: `Application startup failed`

**Solution**:
1. Check backend logs: `docker compose logs backend`
2. Verify environment variables are set
3. Check if database migrations ran successfully

### **5. WebSocket Connection Failed**
**Error**: `WebSocket connection to 'ws://localhost:8080/ws/rides' failed`

**Solution**:
1. Ensure backend is running: `docker compose ps`
2. Check if WebSocket endpoint is accessible
3. Verify CORS configuration

### **6. Test Client Not Working**
**Error**: `Failed to fetch` or `CORS error`

**Solution**:
1. Open browser developer tools (F12)
2. Check Network tab for failed requests
3. Verify API base URL is correct
4. Check if backend is running on correct port

## 🔧 **Debugging Commands**

### **Check Service Status**
```bash
# List all containers and their status
docker compose ps

# Check logs for all services
docker compose logs

# Check logs for specific service
docker compose logs backend
docker compose logs postgres
docker compose logs redis
```

### **Restart Services**
```bash
# Restart all services
docker compose restart

# Restart specific service
docker compose restart backend
```

### **Clean and Rebuild**
```bash
# Stop and remove all containers
docker compose down

# Remove volumes (WARNING: This will delete all data)
docker compose down -v

# Rebuild and start
docker compose up --build -d
```

### **Check Database**
```bash
# Connect to PostgreSQL container
docker compose exec postgres psql -U postgres -d uber_clone

# List tables
\dt

# Check if demo data exists
SELECT * FROM users;
```

## 🧪 **Testing the MVP**

### **1. Health Check**
```bash
# Check if backend is healthy
curl http://localhost:8080/actuator/health

# Expected response:
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"}
  }
}
```

### **2. API Test**
```bash
# Test authentication endpoint
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"rider@demo.com","password":"password"}'

# Expected response:
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {...}
}
```

### **3. Database Test**
```bash
# Check if demo users exist
curl http://localhost:8080/api/users/demo

# Expected response:
{
  "rider": "rider@demo.com",
  "driver": "driver@demo.com"
}
```

## 📋 **Environment Variables**

### **Required Variables**
Make sure these are set in your `backend/.env` file:

```env
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/uber_clone
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=password

# JWT
JWT_SECRET=your_jwt_secret_key_here_make_it_long_and_secure_at_least_256_bits
JWT_EXPIRATION_MS=86400000

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001,http://localhost:8080
```

### **Optional Variables**
```env
# Stripe (for payment testing)
STRIPE_SECRET_KEY=sk_test_placeholder
STRIPE_PUBLISHABLE_KEY=pk_test_placeholder

# ML Service (disabled for MVP)
ML_SERVICE_ENABLED=false
```

## 🚀 **Quick Recovery Steps**

If something goes wrong, follow these steps in order:

1. **Stop all services**: `docker compose down`
2. **Clean up**: `docker compose down -v` (⚠️ deletes data)
3. **Rebuild**: `docker compose up --build -d`
4. **Check health**: `curl http://localhost:8080/actuator/health`
5. **Test API**: Use the test client or curl commands above

## 📞 **Getting Help**

If you're still having issues:

1. **Check the logs**: `docker compose logs`
2. **Verify Docker is running**: `docker --version`
3. **Check ports**: `netstat -ano | findstr :8080`
4. **Restart Docker Desktop** if needed
5. **Create an issue** on GitHub with:
   - Error message
   - Steps to reproduce
   - Your environment (OS, Docker version)
   - Logs from `docker compose logs`

## ✅ **Success Indicators**

Your MVP is working correctly when:

- ✅ `docker compose ps` shows all services as "Up"
- ✅ `curl http://localhost:8080/actuator/health` returns `{"status":"UP"}`
- ✅ Test client can login with demo accounts
- ✅ Swagger UI is accessible at `http://localhost:8080/swagger-ui`
- ✅ Database contains demo users (rider@demo.com, driver@demo.com)

---

**Need more help?** Check the main README.md for detailed setup instructions.
