package club.mrdaisite.torder.torderadmin.service.impl;

import club.mrdaisite.torder.torderadmin.component.CustomException;
import club.mrdaisite.torder.torderadmin.component.WebLogAspect;
import club.mrdaisite.torder.torderadmin.dto.AdminChangeUserPasswordParamDTO;
import club.mrdaisite.torder.torderadmin.dto.UserRegisterParamDTO;
import club.mrdaisite.torder.torderadmin.dto.UserResultDTO;
import club.mrdaisite.torder.torderadmin.service.AdminService;
import club.mrdaisite.torder.torderadmin.util.JwtTokenUtil;
import club.mrdaisite.torder.tordermbg.mapper.UserMapper;
import club.mrdaisite.torder.tordermbg.model.User;
import club.mrdaisite.torder.tordermbg.model.UserExample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * @author dai
 */
@Service
public class AdminServiceImpl implements AdminService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebLogAspect.class);
    private BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private UserMapper userMapper;

    @Override
    public User getAdminByUsername(String username) {
        UserExample userExample = new UserExample();
        userExample.or().andUsernameEqualTo(username);
        List<User> adminList = userMapper.selectByExample(userExample);
        if (adminList != null && adminList.size() > 0) {
            return adminList.get(0);
        }
        return null;
    }

    @Override
    public UserResultDTO register(UserRegisterParamDTO userRegisterParamDTO) {
        User user = new User();
        UserResultDTO userResultDTO = new UserResultDTO();
        BeanUtils.copyProperties(userRegisterParamDTO, user);
        String bCryptPassword = bCryptPasswordEncoder.encode(userRegisterParamDTO.getPassword());
        user.setPassword(bCryptPassword);
        user.setGmtCreate(new Date());
        user.setGmtModified(new Date());
        if (userMapper.insert(user) != 1) {
            return null;
        }
        BeanUtils.copyProperties(user, userResultDTO);
        return userResultDTO;
    }

    @Override
    public String login(String username, String password) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return jwtTokenUtil.generateToken(userDetails);
    }

    @Override
    public Object changeUserPassword(Long id, AdminChangeUserPasswordParamDTO adminChangeUserPasswordParamDTO) throws CustomException {
        User user = userMapper.selectByPrimaryKey(id);
        if (user == null) {
            throw new CustomException("指定的用户不存在");
        }
        String newPassword = bCryptPasswordEncoder.encode(adminChangeUserPasswordParamDTO.getNewPassword());
        user.setPassword(newPassword);
        user.setGmtModified(new Date());
        if (userMapper.insert(user) != 1) {
            return null;
        }
//        BeanUtils.copyProperties(admin, adminResultDTO);
        return null;
    }

    @Override
    public Object getPermissionList(Long userId) {
        return null;
    }
}
