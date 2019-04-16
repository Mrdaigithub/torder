package club.mrdaisite.torder.torderadmin.service.impl;

import club.mrdaisite.torder.torderadmin.component.CustomException;
import club.mrdaisite.torder.torderadmin.dto.UpdatePasswordParamDTO;
import club.mrdaisite.torder.torderadmin.dto.UserInsertParamDTO;
import club.mrdaisite.torder.torderadmin.dto.UserResultDTO;
import club.mrdaisite.torder.torderadmin.service.AdminService;
import club.mrdaisite.torder.torderadmin.util.FuncUtils;
import club.mrdaisite.torder.torderadmin.util.JwtTokenUtil;
import club.mrdaisite.torder.tordermbg.mapper.*;
import club.mrdaisite.torder.tordermbg.model.*;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author dai
 */
@Service
public class AdminServiceImpl implements AdminService {
    private BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private PermissionMapper permissionMapper;
    @Autowired
    private UserRoleRelationMapper userRoleRelationMapper;
    @Autowired
    private RolePermissionRelationMapper rolePermissionRelationMapper;

    @Override
    public List<Object> listUser(Integer page, Integer perPage, String sortBy, String order) {
        PageHelper.startPage(page, perPage, sortBy + " " + order);
        List<User> userList = userMapper.selectByExample(new UserExample());
        PageInfo pageInfo = new PageInfo<>(userList);
        List<Object> pageInfoList = pageInfo.getList();
        List<Object> targetList = new ArrayList<>();
        for (int i = 0; i < pageInfoList.size(); i++) {
            targetList.add(new UserResultDTO());
        }
        return new FuncUtils().beanUtilsCopyListProperties(pageInfoList, targetList);
    }

    @Override
    public UserResultDTO getUserById(Long id) throws CustomException {
        UserResultDTO userResultDTO = new UserResultDTO();
        User user = userMapper.selectByPrimaryKey(id);
        if (user == null) {
            throw new CustomException("不存在的用户");
        }
        BeanUtils.copyProperties(user, userResultDTO);
        return userResultDTO;
    }

    @Override
    public UserResultDTO insertUser(UserInsertParamDTO userInsertParamDTO, String roleName) {
        User user = new User();
        UserRoleRelation userRoleRelation = new UserRoleRelation();
        UserResultDTO userResultDTO = new UserResultDTO();

        RoleExample roleExample = new RoleExample();
        roleExample.or().andNameEqualTo(roleName);
        List<Role> roleList = roleMapper.selectByExample(roleExample);
        Role role = roleList.get(0);
        BeanUtils.copyProperties(userInsertParamDTO, user);

        String bCryptPassword = bCryptPasswordEncoder.encode(userInsertParamDTO.getPassword());
        user.setPassword(bCryptPassword);
        user.setScore(0);
        user.setEnabled(true);
        user.setGmtCreate(new Date());
        user.setGmtModified(new Date());
        userMapper.insert(user);
        userRoleRelation.setUserId(user.getId());
        userRoleRelation.setRoleId(role.getId());
        userRoleRelation.setGmtCreate(new Date());
        userRoleRelation.setGmtModified(new Date());
        userRoleRelationMapper.insert(userRoleRelation);
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
    public Boolean updateUserPassword(Long id, UpdatePasswordParamDTO updatePasswordParamDTO, String roleName) throws AccessDeniedException {
        User user = userMapper.selectByPrimaryKey(id);
        Role role = getRoleByUsername(user.getUsername());
        if (!role.getName().equals(roleName)) {
            throw new AccessDeniedException(null);
        }
        String newPassword = bCryptPasswordEncoder.encode(updatePasswordParamDTO.getNewPassword());
        user.setPassword(newPassword);
        user.setGmtModified(new Date());
        return userMapper.updateByPrimaryKey(user) == 1;
    }

    @Override
    public User getUserByUsername(String username) {
        UserExample userExample = new UserExample();
        userExample.or().andUsernameEqualTo(username);
        List<User> adminList = userMapper.selectByExample(userExample);
        if (adminList != null && adminList.size() > 0) {
            return adminList.get(0);
        }
        return null;
    }

    @Override
    public Role getRoleByUsername(String username) {
        UserExample userExample = new UserExample();
        userExample.or().andUsernameEqualTo(username);
        List<User> userList = userMapper.selectByExample(userExample);
        Long userId = userList.get(0).getId();
        UserRoleRelationExample userRoleRelationExample = new UserRoleRelationExample();
        userRoleRelationExample.or().andUserIdEqualTo(userId);
        List<UserRoleRelation> userRoleRelationList = userRoleRelationMapper.selectByExample(userRoleRelationExample);
        UserRoleRelation userRoleRelation = userRoleRelationList.get(0);
        return roleMapper.selectByPrimaryKey(userRoleRelation.getRoleId());
    }

    @Override
    public Permission getPermissionByPermissionValue(String permissionValue) {
        PermissionExample permissionExample = new PermissionExample();
        permissionExample.or().andValueEqualTo(permissionValue);
        List<Permission> permissionList = permissionMapper.selectByExample(permissionExample);
        return permissionList.get(0);
    }

    @Override
    public List<Permission> getPermissionListByUsername(String username) {
        List<Permission> permissionList = new ArrayList<>();
        User user = getUserByUsername(username);
        Role role = getRoleByUsername(user.getUsername());
        RolePermissionRelationExample rolePermissionRelationExample = new RolePermissionRelationExample();
        rolePermissionRelationExample.or().andRoleIdEqualTo(role.getId());
        List<RolePermissionRelation> rolePermissionRelationList = rolePermissionRelationMapper.selectByExample(rolePermissionRelationExample);
        for (RolePermissionRelation rolePermissionRelation : rolePermissionRelationList) {
            Permission permission = permissionMapper.selectByPrimaryKey(rolePermissionRelation.getPermissionId());
            if (permission != null) {
                permissionList.add(permission);
            }
        }
        return permissionList;
    }
}
