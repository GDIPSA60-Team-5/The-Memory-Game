﻿using Microsoft.AspNetCore.Mvc;
using TheMemoryGame_Backend.Exceptions;
using TheMemoryGame_Backend.Models;
using TheMemoryGame_Backend.Services;

namespace TheMemoryGame_Backend.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class LoginController : ControllerBase
    {
        private readonly UserService _userService;

        public LoginController(UserService userService)
        {
            _userService = userService;
        }

        [HttpGet("by-username/{username}")]
        public IActionResult GetUserByUsername(string username)
        {
            var user = _userService.GetUserByUsername(username);
            if (user == null) return NotFound();
            return Ok(user);
        }

        [HttpPost("login")]
        public IActionResult Login([FromBody] LoginRequest request)
        {
            Console.WriteLine($"Received login: {request.Username}, {request.Password}");
            try
            {
                var user = _userService.AuthenticateUser(request.Username, request.Password);

                HttpContext.Session.SetString("UserId", user.Id.ToString());

                return Ok("Login successful!");
            }
            catch (UserNotFoundException ex)
            {
                return NotFound(ex.Message);
            }
            catch (InvalidPasswordException ex)
            {
                return Unauthorized(ex.Message);
            }
            catch (Exception)
            {
                return StatusCode(500, "An unexpected error occurred.");
            }
        }
    }
}
